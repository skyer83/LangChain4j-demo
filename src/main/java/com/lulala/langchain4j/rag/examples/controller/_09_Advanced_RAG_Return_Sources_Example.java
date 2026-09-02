package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
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
 * 请参考 {@link Naive_RAG_Example} 以了解基本背景。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例演示了如何返回来源（检索到的内容）。
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagReturnSourcesExample")
public class _09_Advanced_RAG_Return_Sources_Example {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/chat")
    public String chat(@RequestParam String query) {
        String relativePath = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
        RagExampleAssistant ragExampleAssisant = createAssistant(relativePath);
        // 我们可以提出如下问题：
        // - 我可以取消预订吗？
        Result<String> result = ragExampleAssisant.answer02(query);
        List<Content> sources = result.sources();
        StringBuilder sb = new StringBuilder();
        sb.append("数据来源（sources）：").append("\n");
        sources.forEach(s -> sb.append(s.toString()).append("\n\n"));
        sb.append("\n\nAiMessage回答（content）：").append("\n").append(result.content());
        return sb.toString();
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

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
//                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
                .build();
    }
}
