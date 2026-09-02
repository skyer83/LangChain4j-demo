package com.lulala.langchain4j.rag.utils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 10:14
 */
@Slf4j
public class RagUtils {

    /**
     * 根据glob模式匹配文件
     * @param glob
     * @return java.nio.file.PathMatcher
     * @author shenjh
     * @since 2026/8/28 10:18
     */
    public static PathMatcher glob(String glob) {
        // glob:*.pdf 匹配当前层级目录下的pdf文件
        // glob:**/*.pdf 匹配子目录下的多级目录下的pdf文件（不匹配当前目录）
        // FileSystemDocumentLoader.loadDocumentsRecursively 配合 "glob:{*.pdf,**/*.pdf}" 实现递归
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    /**
     * 相对路径转Path
     * @param relativePath
     * @return java.nio.file.Path
     * @author shenjh
     * @since 2026/8/28 10:26
     */
    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = RagUtils.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            log.error("相对路径[{}]转换为Path异常", relativePath, e);
            throw new RuntimeException(e);
        }
    }

    public static Document loadDocument(String documentPath) {
        return FileSystemDocumentLoader.loadDocument(toPath(documentPath), new TextDocumentParser());
    }
}
