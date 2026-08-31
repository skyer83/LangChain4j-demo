package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.common.Constants;
import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenScoringModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例展示了一种更高级的 RAG 应用的实现，使用了一种称为"重排序"（re-ranking）的技术。
 * <p>
 * 通常，{@link ContentRetriever} 检索到的结果并非都与用户查询真正相关。
 * 这是因为在初始检索阶段，使用更快、更具成本效益的模型往往更可取，
 * 尤其是在处理大量数据时。
 * 代价是检索质量可能较低。
 * 向大语言模型提供不相关的信息可能会产生高昂的成本，在最坏的情况下还会导致幻觉。
 * 因此，在第二阶段，我们可以对第一阶段获得的结果进行重排序，
 * 并使用更高级的模型（例如 Cohere Rerank）消除不相关的结果。
 * <p>
 * 本示例需要 "langchain4j-cohere" 依赖。（cohere 免费的 API密钥 会报 403，改用 jina（https://jina.ai/） 的 API密钥）<br/>
 * 访问（需要梯子（系统代理）） https://jina.ai/ 后，拉到最下面，可以看到获取 API密钥（免注册科直接获取，但密钥不固定），点击“重排模型”选项卡，可以看到最新重排模型
 * @author shenjh
 * @since 2026/8/31 9:25
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWithReRankingExample")
public class _03_Advanced_RAG_with_ReRanking_Example {

    private static final double MIN_RERANK_SCORE = 0.7;
    private static final int COARSE_RETRIEVAL_MAX_RESULTS = 5;
    private static final int MAX_RERANK_RESULTS = 5;
    private static final int LOG_TEXT_MAX_LENGTH = 120;

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;

    private RagExampleAssistant ragExampleAssisant;

    @RequestMapping("/chat")
    public String chat(@RequestParam String query) {
        if (ragExampleAssisant == null) {
            synchronized (this) {
                if (ragExampleAssisant == null) {
                    String relativePath = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
                    ragExampleAssisant = createAssistant(relativePath);
                }
            }
        }
        // 首先，提问“Hi”（你好）。观察第一阶段检索到的所有片段是如何被全部过滤掉的。
        // 然后，提问“Can I cancel my reservation?”（我可以取消预订吗？），观察除一个片段外，其余片段是如何被过滤掉的。
        return ragExampleAssisant.answer(query);
    }

    private RagExampleAssistant createAssistant(String documentPath) {
        Document document = FileSystemDocumentLoader.loadDocument(RagUtils.toPath(documentPath), new TextDocumentParser());

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .documentSplitter(DocumentSplitters.recursive(300, 50))
                .build();
        ingestor.ingest(document);

        // 先粗排，再精排（此方案需注意：1、Token 消耗量（粗排召回 50 条，每条 1000 Token，一次 Rerank 就会消耗 5万 Token）；2、延迟问题（增加一次网络请求的耗时））
        // 粗排
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(COARSE_RETRIEVAL_MAX_RESULTS)
                .build();


        // To register and get a free API key for Cohere, please visit the following link:
        // https://dashboard.cohere.com/welcome/register
//        String apiKey = System.getenv(Constants.SystemEnv.LANGCHAIN4J_COHERE_API_KEY);
//        ScoringModel scoringModel = CohereScoringModel.builder()
//                .apiKey(apiKey)
//                // 获取模型列表（没有 rerank 模型）：https://docs.cohere.com/reference/list-models?explorer=true
//                // 查看最新使用的 rerank 模型：https://docs.cohere.com/v2/reference/rerank?explorer=true
//                .modelName("rerank-v4.0-pro")
//                .build();
        // 中文 RAG demo 更建议换成 Jina reranker 或本地 ONNX reranker，需要梯子（系统代理）
//        String apiKey = System.getenv(Constants.SystemEnv.LANGCHAIN4J_JINA_API_KEY);
//        ScoringModel scoringModel = JinaScoringModel.builder()
//                .apiKey(apiKey)
//                // 2026-07-27：jina-reranker-v3.5
//                .modelName("jina-reranker-v3.5")
//                .build();
        // cohere 会报 403 Forbidden 错误，jina 需要梯子（系统代理），改用 qwen
        // 使用 DashScope/Qwen reranker 进行重排序。
        // 参见：https://bailian.console.aliyun.com/cn-beijing?tab=model#/model-market/detail/qwen3-rerank
        String apiKey = System.getenv(Constants.SystemEnv.LANGCHAIN4J_DASHSCOPE_API_KEY);
        ScoringModel qwenScoringModel = QwenScoringModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3-rerank")
                .build();
        log.info("初始化精排模型: provider=DashScope/Qwen, modelName=qwen3-rerank, apiKeyPresent={}, coarseMaxResults={}, minScore={}, maxRerankResults={}",
                apiKey != null && !apiKey.isBlank(), COARSE_RETRIEVAL_MAX_RESULTS, MIN_RERANK_SCORE, MAX_RERANK_RESULTS);
        ScoringModel scoringModel = withRerankLogging(qwenScoringModel);
        // 精排
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                // 我们希望仅向大语言模型（LLM）提供与用户查询真正相关的片段。
                .minScore(MIN_RERANK_SCORE)
                .maxResults(MAX_RERANK_RESULTS)
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
    
    /**
     * 打印重排序日志信息
     * @param delegate
     * @return dev.langchain4j.model.scoring.ScoringModel 
     * @author shenjh
     * @since 2026/9/1 13:42
     */
    private ScoringModel withRerankLogging(ScoringModel delegate) {
        // “你好！”的精排结果，
        /*
            初始化精排模型: provider=DashScope/Qwen, modelName=qwen3-rerank, apiKeyPresent=true, coarseMaxResults=5, minScore=0.7, maxRerankResults=5
            开始精排: query=你好！, candidateCount=3, minScore=0.7, maxResults=5
            精排结果: index=0, score=0.43360445274175635, kept=false, text=责任条款xxx...
            精排结果: index=1, score=0.39434119980799, kept=false, text=Miles of Smiles 租车服务使用条款xxx...
            精排结果: index=2, score=0.45050699100794844, kept=false, text=预订xxx...
            精排完成: query=你好！, candidateCount=3, scoreCount=3, keptCount=0, elapsedMs=1426, tokenUsage=TokenUsage { inputTokenCount = 460, outputTokenCount = null, totalTokenCount = 460 }
         */
        // 精排结果最高分 0.45050699100794844，小于最低分要求“MIN_RERANK_SCORE = 0.7”，表示没有片段被保留，符合请求情况
        /*
            HTTP request:
            - method: POST
            - url: https://api.deepseek.com/chat/completions
            - headers: [Authorization: Beare...32], [User-Agent: langchain4j-openai], [Content-Type: application/json]
            - body: {
              "model" : "deepseek-v4-pro",
              "messages" : [ {
                "role" : "user",
                "content" : "你好！"
              } ],
              "stream" : false
            }
         */

        // “我可以取消预订吗？”的精排结果
        /*
            开始精排: query=我可以取消预订吗？, candidateCount=3, minScore=0.7, maxResults=5
            精排结果: index=0, score=0.7613592902571142, kept=true, text=预订xxx...
            精排结果: index=1, score=0.35473721927127855, kept=false, text=责任条款xxx...
            精排结果: index=2, score=0.33727390910291793, kept=false, text=Miles of Smiles 租车服务使用条款xxx...
            精排完成: query=我可以取消预订吗？, candidateCount=3, scoreCount=3, keptCount=1, elapsedMs=201, tokenUsage=TokenUsage { inputTokenCount = 466, outputTokenCount = null, totalTokenCount = 466 }
         */
        // 精排结果最高分 0.7613592902571142，大于等于最低分要求“MIN_RERANK_SCORE = 0.7”，表示该片段被保留，符合请求情况
        /*
            HTTP request:
            - method: POST
            - url: https://api.deepseek.com/chat/completions
            - headers: [Authorization: Beare...32], [User-Agent: langchain4j-openai], [Content-Type: application/json]
            - body: {
              "model" : "deepseek-v4-pro",
              "messages" : [ {
                "role" : "user",
                "content" : "你好！"
              }, {
                "role" : "assistant",
                "content" : "你好！很高兴见到你，有什么可以帮你的吗？"
              }, {
                "role" : "user",
                "content" : "我可以取消预订吗？\n\nAnswer using the following information:\n预订\r\n3.1 用户可通过我们xxx情况下驾驶。"
              } ],
              "stream" : false
            }
         */
        return new ScoringModel() {
            @Override
            public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
                long start = System.currentTimeMillis();
                log.info("开始精排: query={}, candidateCount={}, minScore={}, maxResults={}", query, segments.size(), MIN_RERANK_SCORE, MAX_RERANK_RESULTS);

                Response<List<Double>> response = delegate.scoreAll(segments, query);
                List<Double> scores = response.content();
                int keptCount = 0;
                int logCount = Math.min(segments.size(), scores.size());

                for (int i = 0; i < logCount; i++) {
                    Double score = scores.get(i);
                    boolean kept = score >= MIN_RERANK_SCORE;
                    if (kept) {
                        keptCount++;
                    }
                    log.info("精排结果: index={}, score={}, kept={}, text={}", i, score, kept, abbreviate(segments.get(i).text(), LOG_TEXT_MAX_LENGTH));
                }

                if (scores.size() != segments.size()) {
                    log.warn("精排分数数量与候选片段数量不一致: candidateCount={}, scoreCount={}", segments.size(), scores.size());
                }

                log.info("精排完成: query={}, candidateCount={}, scoreCount={}, keptCount={}, elapsedMs={}, tokenUsage={}",
                        query, segments.size(), scores.size(), keptCount, System.currentTimeMillis() - start, response.tokenUsage());

                if (RETURN_MESSAGE_WHEN_NO_RELEVANT_CONTENT.get() && keptCount == 0) {
                    // 若要求精排后没有符合要求的文档片段时返回提示信息，则抛出 NoRelevantContentException 异常
                    throw new NoRelevantContentException();
                }

                return response;
            }
        };
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static final String NO_RELEVANT_CONTENT_MESSAGE = "未匹配到相关信息，请联系客服";
    private static final ThreadLocal<Boolean> RETURN_MESSAGE_WHEN_NO_RELEVANT_CONTENT = ThreadLocal.withInitial(() -> false);

    private RagExampleAssistant ragExampleAssisant02;
    @RequestMapping("/chat02")
    public String chat02(@RequestParam String query) {
        if (ragExampleAssisant02 == null) {
            synchronized (this) {
                if (ragExampleAssisant02 == null) {
                    String relativePath = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
                    ragExampleAssisant02 = createAssistant02(relativePath);
                }
            }
        }
        // 首先，提问“Hi”（你好）。观察第一阶段检索到的所有片段是如何被全部过滤掉的。
        // 然后，提问“Can I cancel my reservation?”（我可以取消预订吗？），观察除一个片段外，其余片段是如何被过滤掉的。
        RETURN_MESSAGE_WHEN_NO_RELEVANT_CONTENT.set(true);
        try {
            return ragExampleAssisant02.answer(query);
        } catch (NoRelevantContentException e) {
            log.info("精排后没有符合要求的文档片段: query={}", query);
            return NO_RELEVANT_CONTENT_MESSAGE;
        } finally {
            RETURN_MESSAGE_WHEN_NO_RELEVANT_CONTENT.remove();
        }
    }

    private RagExampleAssistant createAssistant02(String documentPath) {
        Document document = FileSystemDocumentLoader.loadDocument(RagUtils.toPath(documentPath), new TextDocumentParser());

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .documentSplitter(DocumentSplitters.recursive(300, 50))
                .build();
        ingestor.ingest(document);

        // 先粗排，再精排（此方案需注意：1、Token 消耗量（粗排召回 50 条，每条 1000 Token，一次 Rerank 就会消耗 5万 Token）；2、延迟问题（增加一次网络请求的耗时））
        // 粗排
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(COARSE_RETRIEVAL_MAX_RESULTS)
                .build();

        // 使用 DashScope/Qwen reranker 进行重排序。
        String apiKey = System.getenv(Constants.SystemEnv.LANGCHAIN4J_DASHSCOPE_API_KEY);
        ScoringModel qwenScoringModel = QwenScoringModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3-rerank")
                .build();
        log.info("初始化精排模型: provider=DashScope/Qwen, modelName=qwen3-rerank, apiKeyPresent={}, coarseMaxResults={}, minScore={}, maxRerankResults={}",
                apiKey != null && !apiKey.isBlank(), COARSE_RETRIEVAL_MAX_RESULTS, MIN_RERANK_SCORE, MAX_RERANK_RESULTS);
        ScoringModel scoringModel = withRerankLogging(qwenScoringModel);
        // 精排
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                // 我们希望仅向大语言模型（LLM）提供与用户查询真正相关的片段。
                .minScore(MIN_RERANK_SCORE)
                .maxResults(MAX_RERANK_RESULTS)
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static class NoRelevantContentException extends RuntimeException {
    }
}
