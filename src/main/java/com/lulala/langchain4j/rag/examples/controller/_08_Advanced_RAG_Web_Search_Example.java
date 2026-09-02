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
 * 本示例演示了如何将网络搜索引擎用作额外的内容检索器。
 * <p>
 * 本示例需要 "langchain4j-web-search-engine-tavily" 依赖。
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWebSearchExample")
public class _08_Advanced_RAG_Web_Search_Example {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private ContentRetriever contentRetriever4Web;

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
        // 首先，提问“川普是谁？”
        // 然后，提问“Can I cancel my reservation?”（我可以取消预订吗？）
        // 现在，观察日志。

        // 川普是谁？（2 条来自文档，3 条来自网络搜索）
        /*
            UserMessage { name = null, contents = [TextContent { text = "川普是谁？

            Answer using the following information:
            Miles of Smiles 租车服务使用条款

            简介
            本服务条款（以下简称“条款”）...。

            唐纳德·特朗普家族 - 维基百科，自由的百科全书
            唐纳·川普（Donald Trump）是特朗普集团的所有者...

            责任条款
            6.1 用户对租赁期间发生的任何损坏、丢失或盗窃承担责任...。

            唐纳德·特朗普 - 维基百科
            ...
            维基百科，自由的百科全书

            “川普”和“特朗普”均重定向至此。关于其他用法，...。" }]
         */
        // 我可以取消预订吗？
        /*
            UserMessage { name = null, contents = [TextContent { text = "我可以取消预订吗？

            Answer using the following information:
            预订
            3.1 用户可通过我们的网站或移动应用程序进行预订...。

            取消政策
            4.1 用户可在预订起始日期前至少7天取消预订。
            4.2 若预订租期少于3天，则不允许取消。

            车辆使用规定
            5.1 从 Miles of Smiles 租赁的所有车辆...。

            我可以取消预订吗？ - 常见问题解答E-VAI 汽车共享
            是的，可以取消预订。但是，如果您想取消预订，...

            责任条款
            6.1 用户对租赁期间发生的任何损坏、丢失或盗窃承担责任...。

            取消/ 更改網上預訂| 常見問題
            您可輕鬆更改或取消您的預訂。只需輸入...

            「不可退款」酒店如何取消？成功爭取退款的6個實用方法
            當退款與改期都無望時，轉讓訂房是一個值得考慮的退路。..." }]
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

        // 我们来创建一个查询路由器，它会将每个查询同时路由给两个检索器。
        // contentRetriever01 提供 2 条检索结果
        // contentRetriever4Web 提供 3 条检索结果
        QueryRouter queryRouter = new DefaultQueryRouter(contentRetriever01, contentRetriever4Web);

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
