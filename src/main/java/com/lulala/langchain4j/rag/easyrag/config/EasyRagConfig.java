package com.lulala.langchain4j.rag.easyrag.config;

import cn.hutool.core.util.StrUtil;
import com.knuddels.jtokkit.api.ModelType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/25 11:32
 */
@Slf4j
@Configuration
public class EasyRagConfig {

    public static final class BeanName {
        public static final String EMBEDDING_MODEL_OF_ZH_V15 = "embeddingModelOfZhV15";

        public static final String EMBEDDING_STORE_4_EASY_RAG = "embeddingStore4EasyRag";
        public static final String CONTENT_RETRIEVER_4_EASY_RAG = "contentRetriever4EasyRag";

        public static final String EMBEDDING_STORE_4_TRANSFORMER = "embeddingStore4Transformer";
        public static final String CONTENT_RETRIEVER_4_TRANSFORMER = "contentRetriever4Transformer";
    }

    @Bean(BeanName.EMBEDDING_MODEL_OF_ZH_V15)
    EmbeddingModel embeddingModelOfZhV15() {
        return new BgeSmallZhV15EmbeddingModel();
    }

    @Bean(BeanName.EMBEDDING_STORE_4_EASY_RAG)
    InMemoryEmbeddingStore<TextSegment> embeddingStore4EasyRag(
            @Qualifier(BeanName.EMBEDDING_MODEL_OF_ZH_V15) EmbeddingModel embeddingModelOfZhV15) {

        // glob:*.pdf 匹配当前层级目录下的pdf文件
        // glob:**/*.pdf 匹配子目录下的多级目录下的pdf文件（不匹配当前目录）
        // FileSystemDocumentLoader.loadDocumentsRecursively 配合 "glob:{*.pdf,**/*.pdf}" 实现递归
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{*.pdf,**/*.pdf}");

        // 按文件名后缀匹配，兼容 Windows 下根目录 PDF 和子目录 PDF。配合 FileSystemDocumentLoader.loadDocumentsRecursively 实现递归匹配
//        PathMatcher pathMatcher = path -> path.getFileName()
//                .toString()
//                .toLowerCase(Locale.ROOT)
//                .endsWith(".pdf");

        // 加了环境变量后，IDEA 工具要完全重启才能取到环境变量的值
        String directoryPath = System.getenv("LANGCHAIN4J_DEMO_DOC_PATH");
        if (StrUtil.isBlank(directoryPath)) {
            throw new IllegalStateException("环境变量 LANGCHAIN4J_DEMO_DOC_PATH 未配置");
        }
        log.info("加载知识库文档目录: {}", directoryPath);
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(directoryPath, pathMatcher);
        if (documents.isEmpty()) {
            log.warn("知识库目录中没有找到 PDF 文档: {}", directoryPath);
            return embeddingStore;
        }
        List<Document> searchableDocuments = documents.stream()
                .filter(document -> document.text() != null && !document.text().isBlank())
                .toList();
        if (searchableDocuments.isEmpty()) {
            log.warn("知识库 PDF 没有可抽取文本，可能是扫描件。请先 OCR 或转换为带文本层的 PDF: {}", directoryPath);
            return embeddingStore;
        }
        if (searchableDocuments.size() < documents.size()) {
            log.warn("忽略 {} 个没有可抽取文本的 PDF 文档", documents.size() - searchableDocuments.size());
        }
        log.info("知识库加载完成，可检索文档数量: {}", searchableDocuments.size());

        IngestionResult ingestionResult = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModelOfZhV15)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(searchableDocuments);
        log.info("知识库提取完成，提取结果: {}", ingestionResult.tokenUsage().toString());
        return embeddingStore;
    }

    @Bean(BeanName.EMBEDDING_STORE_4_TRANSFORMER)
    InMemoryEmbeddingStore<TextSegment> embeddingStore4Transformer(
            @Qualifier(BeanName.EMBEDDING_MODEL_OF_ZH_V15) EmbeddingModel embeddingModelOfZhV15) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{*.pdf,**/*.pdf}");

        // 加了环境变量后，IDEA 工具要完全重启才能取到环境变量的值
        String directoryPath = System.getenv("LANGCHAIN4J_DEMO_DOC_PATH");
        if (StrUtil.isBlank(directoryPath)) {
            throw new IllegalStateException("环境变量 LANGCHAIN4J_DEMO_DOC_PATH 未配置");
        }
        log.info("加载知识库文档目录: {}", directoryPath);
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(directoryPath, pathMatcher);
        if (documents.isEmpty()) {
            log.warn("知识库目录中没有找到 PDF 文档: {}", directoryPath);
            return embeddingStore;
        }
        List<Document> searchableDocuments = documents.stream()
                .filter(document -> document.text() != null && !document.text().isBlank())
                .toList();
        if (searchableDocuments.isEmpty()) {
            log.warn("知识库 PDF 没有可抽取文本，可能是扫描件。请先 OCR 或转换为带文本层的 PDF: {}", directoryPath);
            return embeddingStore;
        }
        if (searchableDocuments.size() < documents.size()) {
            log.warn("忽略 {} 个没有可抽取文本的 PDF 文档", documents.size() - searchableDocuments.size());
        }
        log.info("知识库加载完成，可检索文档数量: {}", searchableDocuments.size());

        IngestionResult ingestionResult = EmbeddingStoreIngestor.builder()
                // 为每个 Document 添加 userId 元数据条目，以便稍后可以按其过滤
                .documentTransformer(document -> {
                    // documentTransformer document: DefaultDocument { text = "完整的文档内容", metadata = Metadata { metadata = {absolute_directory_path=C:\systemEnv\xxx\1, file_name=xxx.pdf} } }
                    //log.info("documentTransformer document: {}", document);
                    Metadata metadata = document.metadata();
                    metadata.put("userId", "123456");
                    // documentTransformer metadata: Metadata { metadata = {absolute_directory_path=C:\systemEnv\xxx\1, userId=123456, file_name=xxx.pdf} }
                    //log.info("documentTransformer metadata: {}", metadata);
                    return document;
                })
                // 将每个 Document 拆分为 1000 个 token 的 TextSegment，重叠 200 个 token
                .documentSplitter(DocumentSplitters.recursive(
                        1000, 200,
                        new OpenAiTokenCountEstimator(ModelType.GPT_4O_MINI.getName())))
                // 将 Document 的名称添加到每个 TextSegment 中以提高搜索质量
                .textSegmentTransformer(textSegment -> {
                    // textSegmentTransformer textSegment: TextSegment { text = "拆分后的文本内容（1000 个 token 的 TextSegment）" metadata = {absolute_directory_path=C:\systemEnv\xxx\1, index=0, userId=123456, file_name=xxx.pdf} }
                    //log.info("textSegmentTransformer textSegment: {}", textSegment);
                    Metadata metadata = textSegment.metadata();
                    String fileNameInfo = metadata.getString("file_name") + "\n" + textSegment.text();
                    TextSegment newTextSegment = TextSegment.from(fileNameInfo, metadata);
                    // textSegmentTransformer TextSegment.from: TextSegment { text = "xxx.pdf\n拆分后的文本内容" metadata = {absolute_directory_path=C:\systemEnv\xxx\1, index=0, userId=123456, file_name=xxx.pdf} }
                    //log.info("textSegmentTransformer TextSegment.from: {}", newTextSegment);
                    return newTextSegment;
                })
                .embeddingModel(embeddingModelOfZhV15)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(searchableDocuments);
        log.info("知识库提取完成，提取结果: {}", ingestionResult.tokenUsage().toString());
        return embeddingStore;
    }

    /*
        dev.langchain4j.rag.spring.RagAutoConfig 会在项目里没有 ContentRetriever Bean 时自动创建一个默认 contentRetriever，
        但现在项目有多个 EmbeddingStore（embeddingStore4EasyRag、embeddingStore4Transformer），Spring 不知道默认检索器该用哪个，所以启动失败报错：
            Parameter 1 of method contentRetriever in dev.langchain4j.rag.spring.RagAutoConfig required a single bean, but 2 were found:
            - embeddingStore4EasyRag: defined by method 'embeddingStore4EasyRag' in class path resource [com/lulala/langchain4j/rag/easyrag/config/EasyRagConfig.class]
            - embeddingStore4Transformer: defined by method 'embeddingStore4Transformer' in class path resource [com/lulala/langchain4j/rag/easyrag/config/EasyRagConfig.class]

        现在自定义 ContentRetriever Bean，这样 RagAutoConfig 会因为已经存在 ContentRetriever Bean 而退让，不再自动创建默认检索器，也就不会再卡在多个 EmbeddingStore 上。
     */
    @Bean(BeanName.CONTENT_RETRIEVER_4_EASY_RAG)
    ContentRetriever contentRetriever4EasyRag(
            @Qualifier(BeanName.EMBEDDING_MODEL_OF_ZH_V15) EmbeddingModel embeddingModelOfZhV15,
            @Qualifier(BeanName.EMBEDDING_STORE_4_EASY_RAG) EmbeddingStore<TextSegment> embeddingStore4EasyRag) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore4EasyRag)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(5)
                .minScore(0.25)
                .build();
    }

    @Bean(BeanName.CONTENT_RETRIEVER_4_TRANSFORMER)
    ContentRetriever contentRetriever4Transformer(
            @Qualifier(BeanName.EMBEDDING_MODEL_OF_ZH_V15) EmbeddingModel embeddingModelOfZhV15,
            @Qualifier(BeanName.EMBEDDING_STORE_4_TRANSFORMER) EmbeddingStore<TextSegment> embeddingStore4Transformer) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore4Transformer)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(5)
                .minScore(0.25)
                .build();
    }

}
