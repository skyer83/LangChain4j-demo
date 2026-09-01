package com.lulala.langchain4j.rag.examples.controller;

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
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
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

import java.util.Arrays;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例说明了如何将文档来源和其他元数据包含到 LLM 提示词中。
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 15:07
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWithMetadataExample")
public class _04_Advanced_RAG_with_Metadata_Example {

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
        // 提问：“定义取消政策的文件叫什么名字？”
        // 观察 “file_name”（文件名）元数据是如何被注入到提示词（prompt）中的。
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
                .build();

        // 每个检索到的文本片段都应在提示词（prompt）中包含 file_name（文件名）和 index（索引）的元数据值
        ContentInjector contentInjector = DefaultContentInjector.builder()
                // 修改排版/格式
                //.promptTemplate(new PromptTemplate("Source: {file_name}, Segment: {index}\n{content}"))
                .metadataKeysToInclude(Arrays.asList("file_name", "index"))
                .build();

        // 提示词注入效果
        /*
            [UserMessage { name = null, contents = [TextContent { text = "定义取消政策的文件叫什么名字？

            Answer using the following information:
            content: 预订
            3.1 用户可通过我们的网站或移动应用程序进行预订。
            ...
            file_name: miles-of-smiles-terms-of-use.txt
            index: 1

            content: Miles of Smiles 租车服务使用条款

            简介
            本服务条款（以下简称“条款”）规范您（个人用户）...
            file_name: miles-of-smiles-terms-of-use.txt
            index: 0

            content: 责任条款
            6.1 用户对租赁期间发生的任何损坏、丢失或盗窃承担责任。
            ...
            file_name: miles-of-smiles-terms-of-use.txt
            index: 2" }], attributes = {} }]
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
