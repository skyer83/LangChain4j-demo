package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例演示了如何有条件地跳过检索。
 * 有时，检索是不必要的，例如当用户只是说"Hi"（你好）时。
 * <p>
 * 实现此功能有多种方式，但最简单的方式是使用自定义的 {@link QueryRouter}。
 * 当需要跳过检索时，QueryRouter 将返回一个空列表，
 * 这意味着该查询不会被路由到任何 {@link ContentRetriever}。
 * <p>
 * 决策可以通过多种方式实现：
 * - 使用规则（例如，根据用户的权限、位置等）。
 * - 使用关键词（例如，如果查询包含特定词汇）。
 * - 使用语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
 * - 使用 LLM 来做决策。
 * <p>
 * 在本示例中，我们将使用 LLM 来决定用户的查询是否需要进行检索。
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 15:07
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagSkipRetrievalExample")
public class _06_Advanced_RAG_Skip_Retrieval_Example {

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
        // 首先，说 "Hi"（你好）
        // 注意观察，这个查询没有被路由到任何检索器（retrievers）。

        // 现在，提问 "Can I cancel my reservation?"（我可以取消预订吗？）
        // 这个查询已经被路由到了我们的检索器。
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

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建路由
        QueryRouter queryRouter = new QueryRouter() {
            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from("""
                    以下查询是否与汽车租赁公司的业务相关？
                    只回答 '是'、'否'或 '也许'。
                    查询：{{it}}
                    """);
            @Override
            public Collection<ContentRetriever> route(Query query) {
                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());
                AiMessage aiMessage = deepseekChatModel.chat(prompt.toUserMessage()).aiMessage();
                log.info("Skip_Retrieval_Example LLM decided: {}", aiMessage.text());
                if (aiMessage.text().contains("否")) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(contentRetriever);
            }
        };

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
