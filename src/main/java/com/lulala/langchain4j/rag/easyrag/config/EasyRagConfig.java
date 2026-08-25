package com.lulala.langchain4j.rag.easyrag.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
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

    @Bean
    EmbeddingModel embeddingModel() {
        return new BgeSmallZhV15EmbeddingModel();
    }

    @Bean
    InMemoryEmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {

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

        EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(searchableDocuments);
        log.info("知识库加载完成，可检索文档数量: {}", searchableDocuments.size());
        return embeddingStore;
    }

}
