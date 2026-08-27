package com.lulala.langchain4j.rag.ragapi.controller;

import com.lulala.langchain4j.rag.easyrag.service.EasyRagAssistant;
import dev.langchain4j.community.data.document.graph.GraphDocument;
import dev.langchain4j.community.data.document.graph.GraphEdge;
import dev.langchain4j.community.data.document.graph.GraphNode;
import dev.langchain4j.community.data.document.transformer.graph.GraphTransformer;
import dev.langchain4j.community.data.document.transformer.graph.LLMGraphTransformer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InterruptedIOException;
import java.util.Set;

/**
 * 参考：https://langchain4j.cn/tutorials/rag.html
 * @author shenjh
 * @version 1.0
 * @since 2026/8/26 11:25
 */
@Slf4j
@RestController
@RequestMapping("/rag/ragapi")
public class RagApiController {

    @Autowired
    private ContentRetriever contentRetriever4Web;
    @Autowired
    private ChatModel deepseekChatModel;
    
    /**
     * Graph Transformer
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/8/27 15:34
     */
    @RequestMapping("/graphTransformer")
    public String graphTransformer() {
        GraphTransformer graphTransformer = LLMGraphTransformer.builder()
                .model(deepseekChatModel)
                // 当前版本构造器要求 examples 非空；不使用 few-shot 示例时传空字符串。
                .examples("")
                .additionalInstructions("只抽取文本中明确出现的信息，不要推断。")
                .maxAttempts(3)
                .build();
        // 输入文档
        Document document = Document.from("巴拉克·奥巴马出生于夏威夷，曾担任美国第44任总统。");
        // 转换文档
        GraphDocument graphDocument = graphTransformer.transform(document);
        // 访问节点和关系
        Set<GraphNode> graphNodes = graphDocument.nodes();
        Set<GraphEdge> graphEdges = graphDocument.relationships();

        StringBuilder stringBuilder = new StringBuilder();
        graphNodes.forEach(graphNode -> {stringBuilder.append(graphNode.toString()).append("\n");});
        graphEdges.forEach(graphEdge -> {stringBuilder.append(graphEdge.toString()).append("\n");});

        log.info(stringBuilder.toString());

        return stringBuilder.toString();
    }
    
    /**
     * 网络搜索内容检索器（Web Search Content Retriever）
     * @param message
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/8/27 15:33
     */
    @RequestMapping("/webSearchOfTavily")
    public String webSearchOfTavily(@RequestParam String message) {
        EasyRagAssistant easyRagAssistant = AiServices.builder(EasyRagAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever4Web)
                .build();
        try {
            return easyRagAssistant.chat(message);
        } catch (RuntimeException e) {
            if (isTimeoutException(e)) {
                log.warn("Tavily 网络搜索超时: {}", message, e);
                return "Tavily 网络搜索超时，请稍后重试或缩小问题范围。";
            }
            throw e;
        }
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof InterruptedIOException) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
