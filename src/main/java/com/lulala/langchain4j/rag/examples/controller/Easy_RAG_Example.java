package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssisant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 本示例演示了如何实现一个“简易 RAG”（检索增强生成）应用。
 * 所谓“简易”，是指我们无需深入探讨解析、切分、嵌入等所有细节。
 * 所有的“魔法”都封装在 “langchain4j-easy-rag” 模块中。
 * 如果您想了解如何在不依赖“简易 RAG”封装“魔法”的情况下实现 RAG，请参阅 {@link Naive_RAG_Example}。
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 10:06
 */
@Slf4j
@RestController
@RequestMapping("/rag/examples")
public class Easy_RAG_Example {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;

    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/chat")
    public String chat(@RequestParam String query) {
        String relativePath = "rag-examples/documents";
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(RagUtils.toPath(relativePath), RagUtils.glob("*.txt"));

        RagExampleAssisant ragExampleAssisant = AiServices.builder(RagExampleAssisant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever(documents, embeddingModelOfZhV15))
                .build();
        // 我们可以提出如下问题：
        // - 我可以取消预订吗？
        // - 我出了事故，需要额外付费吗？
        return ragExampleAssisant.answer(query);
    }

    private static ContentRetriever createContentRetriever(List<Document> documents, EmbeddingModel embeddingModel) {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(documents);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }
}
