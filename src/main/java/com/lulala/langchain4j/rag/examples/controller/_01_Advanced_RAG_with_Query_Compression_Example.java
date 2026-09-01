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
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
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
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例展示了一种更复杂的 RAG 应用的实现，使用了一种称为"查询压缩"（query compression）的技术。
 * 通常，用户的查询是后续问题，会引用对话的先前部分，并且缺乏有效检索所需的全部细节。
 * 例如，考虑以下对话：
 * 用户：John Doe 的遗产是什么？
 * AI：John Doe 是……
 * 用户：他什么时候出生的？
 * <p>
 * 在这种情况下，使用基本 RAG 方法并像"When was he born?"这样的查询
 * 很可能无法找到关于 John Doe 的文章，因为查询中并不包含"John Doe"。
 * 查询压缩的做法是：获取用户的查询和前面的对话内容，然后要求大语言模型（LLM）
 * 将其"压缩"为一个单一的、自包含的查询。
 * LLM 应生成类似"When was John Doe born?"的查询。
 * 这种方法会引入一定的延迟和成本，但能显著提升 RAG 过程的质量。
 * 值得注意的是，用于压缩的 LLM 不必与用于对话的 LLM 相同。例如，您可以使用一个专为摘要任务训练的较小本地模型。
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 15:07
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWithQueryCompressionExample")
public class _01_Advanced_RAG_with_Query_Compression_Example {

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
                    String relativePath = "rag-examples/documents/biography-of-john-doe.txt";
                    ragExampleAssisant = createAssistant(relativePath);
                }
            }
        }
        // 首先，提问“What is the legacy of John Doe?”（约翰·多伊的深远影响是什么？）
        // 然后，提问“When was he born?”（他什么时候出生的？）
        // 现在，查看日志：
        // 由于没有先前的上下文，第一个查询没有被压缩。
        // 然而，第二个查询被压缩成了类似“When was John Doe born?”（约翰·多伊什么时候出生的？）这样的独立问题。
        /*
            第二次请求内容会组装为：
                Read and understand the conversation between the User and the AI. Then, analyze the new query from the User.
                Identify all relevant details, terms, and context from both the conversation and the new query.
                Reformulate this query into a clear, concise, and self-contained format suitable for information retrieval.
                （中文：请阅读并理解用户与 AI 之间的对话。然后，分析用户的新查询。从对话和新查询中识别出所有相关的细节、术语和上下文。将该查询重新表述为清晰、简洁且自包含的格式，以便于信息检索。）

                Conversation:
                User: 约翰·多伊的深远影响是什么？

                Answer using the following information:
                慈善事业与个人生活xxx...

                AI: 约翰·多伊的深远影响在于他留下了多维度的遗产：他既是开拓性的工程师xxx...

                User query: 他什么时候出生的？

                It is very important that you provide only reformulated query and nothing else! Do not prepend a query with anything!
                （中文：请务必仅提供改写后的查询，不要包含任何其他内容！不要在查询前添加任何前缀！）
         */
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

        // 我们将创建一个 CompressingQueryTransformer（压缩查询转换器），
        // 它负责将用户的查询和先前的对话压缩为一个单一的、独立的查询。
        // 这应当能显著提升检索过程的质量。
        QueryTransformer queryTransformer = new CompressingQueryTransformer(deepseekChatModel);

        // RetrievalAugmentor（检索增强器）是 LangChain4j 中 RAG 流程的入口。
        // 可以对其进行配置，以根据您的具体需求自定义 RAG 行为。
        // 在后续的示例中，我们将探索更多的自定义选项。
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .queryTransformer(queryTransformer)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
