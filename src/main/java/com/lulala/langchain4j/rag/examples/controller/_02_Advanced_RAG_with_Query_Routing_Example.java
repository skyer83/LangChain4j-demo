package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例展示了一种更高级的 RAG 应用的实现，使用了一种称为"查询路由"（query routing）的技术。
 * <p>
 * 通常，私有数据分散在多个来源和格式中。
 * 这可能包括 Confluence 上的公司内部文档、Git 仓库中的项目代码、
 * 存储用户数据的关系型数据库，或包含你所售产品的搜索引擎等。
 * 在利用多个来源数据的 RAG 流程中，你很可能拥有多个
 * {@link EmbeddingStore} 或 {@link ContentRetriever}。
 * 虽然你可以将每个用户查询路由到所有可用的 {@link ContentRetriever}，
 * 但这种做法可能效率低下且适得其反。
 * <p>
 * "查询路由"是解决这一挑战的方案。它涉及将查询定向到最合适的
 * {@link ContentRetriever}（或多个）。路由可以通过多种方式实现：
 * - 使用规则（例如，根据用户的权限、位置等）。
 * - 使用关键词（例如，如果查询包含 X1、X2、X3 等词汇，则将其路由到对应的 {@link ContentRetriever} X）。
 * - 使用语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
 * - 使用大语言模型（LLM）来做出路由决策。
 * <p>
 * 对于场景 1、2 和 3，你可以实现自定义的 {@link QueryRouter}。
 * 对于场景 4，本示例将演示如何使用 {@link LanguageModelQueryRouter}。
 * <p>
 * @author shenjh
 * @since 2026/8/31 9:25
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWithQueryRoutingExample")
public class _02_Advanced_RAG_with_Query_Routing_Example {

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
        // 现在，查看日志，观察这些查询是如何被路由到不同的检索器（retrievers）的。
        return ragExampleAssisant.answer(query);
    }

    private RagExampleAssistant createAssistant() {
        // 让我们创建一个专门用于存储传记的独立嵌入存储。
        String biographyOfJohnDoePath = "rag-examples/documents/biography-of-john-doe.txt";
        EmbeddingStore<TextSegment> biographyOfJohnDoeEmbeddingStore = embed(RagUtils.toPath(biographyOfJohnDoePath), embeddingModelOfZhV15);
        ContentRetriever biographyOfJohnDoeContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyOfJohnDoeEmbeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.6)
                .build();
        // 此外，再创建一个专门用于存储使用条款的独立嵌入存储。
        String milesOfSmilesTermsOfUsePath = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
        EmbeddingStore<TextSegment> milesOfSmilesTermsOfUseEmbeddingStore = embed(RagUtils.toPath(milesOfSmilesTermsOfUsePath), embeddingModelOfZhV15);
        ContentRetriever milesOfSmilesTermsOfUseContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(milesOfSmilesTermsOfUseEmbeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.6)
                .build();
        // 接下来，让我们创建一个查询路由器。
        Map<ContentRetriever, String> retrieverToDescriptionMap = Map.of(
                biographyOfJohnDoeContentRetriever, "约翰·多伊的传记",
                milesOfSmilesTermsOfUseContentRetriever, "Miles of Smiles 的使用条款");
        QueryRouter queryRouter = new LanguageModelQueryRouter(deepseekChatModel, retrieverToDescriptionMap);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = FileSystemDocumentLoader.loadDocument(documentPath, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}
