package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
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

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本背景。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例演示了如何使用多个内容检索器。
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagMultipleRetrieversExample")
public class _07_Advanced_RAG_Multiple_Retrievers_Example {

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
                    ragExampleAssisant = createAssistant();
                }
            }
        }
        // 首先，提问“What is the legacy of John Doe?”（约翰·多伊的深远影响是什么？）
        // 然后，提问“Can I cancel my reservation?”（我可以取消预订吗？）
        // 现在，观察日志。

        // 约翰·多伊的深远影响是什么？（两个检索器检索到的信息揉到了一起）
        /*
            UserMessage { name = null, contents = [TextContent { text = "约翰·多伊的深远影响是什么？

            Answer using the following information:
            Miles of Smiles 租车服务使用条款

            简介
            本服务条款...。

            服务内容
            Miles of Smiles 向最终用户...。

            慈善事业与个人生活
            约翰·多伊在商界与文坛的双重成功...。

            深远影响
            约翰·多伊的人生留下了多维度的遗产：他是开拓性的工程师、备受赞誉的作家，
            也是无私奉献的慈善家。
            他在科技与文学领域的卓越贡献，在世界范围内留下了不可磨灭的印记，
            激励着无数人满怀激情与决心去追逐梦想。

            责任条款
            6.1 用户对租赁期...。

            约翰·多伊：一位虚构的杰出人物

            早年生活与教育
            约翰·多伊于1980年4月1日出生在美...。" }]
         */
        // 我可以取消预订吗？（两个检索器检索到的信息揉到了一起
        /*
            UserMessage { name = null, contents = [TextContent { text = "我可以取消预订吗？

            Answer using the following information:
            预订
            3.1 用户可通过我...。

            取消政策
            4.1 用户可在预订起始日期前至少7天取消预订。
            4.2 若预订租期少于3天，则不允许取消。

            车辆使用规定
            5.1 从 Miles of Smiles 租赁的所有...。

            然而，他对写作的热情从未减退。
            业余时间，约翰创作短篇小说，其中...。

            责任条款
            6.1 用户对租赁期间发生...。

            慈善事业与个人生活
            约翰·多伊在商界与文坛的双重成功，并未掩盖...。" }]
         */
        return ragExampleAssisant.answer(query);
    }

    private RagExampleAssistant createAssistant() {
        String documentPath01 = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
        Document document01 = RagUtils.loadDocument(documentPath01);

        EmbeddingStore<TextSegment> embeddingStore01 = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor01 = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore01)
                .embeddingModel(embeddingModelOfZhV15)
                .documentSplitter(DocumentSplitters.recursive(300, 50))
                .build();
        ingestor01.ingest(document01);

        ContentRetriever contentRetriever01 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore01)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.6)
                .build();

        String documentPath02 = "rag-examples/documents/biography-of-john-doe.txt";
        Document document02 = RagUtils.loadDocument(documentPath02);

        EmbeddingStore<TextSegment> embeddingStore02 = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor02 = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore02)
                .embeddingModel(embeddingModelOfZhV15)
                .documentSplitter(DocumentSplitters.recursive(300, 50))
                .build();
        ingestor02.ingest(document02);

        ContentRetriever contentRetriever02 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore02)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 我们来创建一个查询路由器，它会将每个查询同时路由给两个检索器。
        QueryRouter queryRouter = new DefaultQueryRouter(contentRetriever01, contentRetriever02);

        // _02_Advanced_RAG_with_Query_Routing_Example 与 _07_Advanced_RAG_Multiple_Retrievers_Example 路由区别
        /*
            两个类都用了两个 ContentRetriever，但路由策略完全不同：_02 是让 LLM 选择检索器，_07 是默认广播到所有检索器。
            _02：智能路由，先判断再检索。
                它会先让 LLM 根据问题和检索器描述做选择，
                所以用户问“约翰·多伊是谁？”时，理论上只查传记库；
                问“我可以取消预订吗？”时，理论上只查使用条款库。
                特点：更智能、更省检索成本，但会多一次 LLM 调用，并且路由结果依赖模型判断。
            _07：多检索器广播，全部都检索。
                它不会判断问题属于哪个知识库，而是每次都把同一个查询发给所有 ContentRetriever。
                也就是说，无论用户问“约翰·多伊是谁？”还是“我可以取消预订吗？”，两个检索器都会被查询，然后结果一起进入后续聚合流程。
                特点：简单、稳定、无额外 LLM 路由调用，但容易混入无关文档片段。
            总结：
                _02 是“按问题选择合适的检索器”；
                _07 是“所有检索器都查一遍”。
                如果你的知识库主题差异明显，用 _02 更合适；
                如果多个检索器都可能提供补充信息，用 _07 更直接。
         */

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
