package com.lulala.langchain4j.rag.ragapi.controller;

import dev.langchain4j.community.data.document.graph.GraphDocument;
import dev.langchain4j.community.data.document.graph.GraphEdge;
import dev.langchain4j.community.data.document.graph.GraphNode;
import dev.langchain4j.community.data.document.transformer.graph.GraphTransformer;
import dev.langchain4j.community.data.document.transformer.graph.LLMGraphTransformer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/26 11:25
 */
@Slf4j
@RestController
@RequestMapping("/rag/ragapi")
public class RagApiController {

    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/graphTransformer")
    public String graphTransformer() {
        GraphTransformer graphTransformer = LLMGraphTransformer.builder()
                .model(deepseekChatModel)
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
}
