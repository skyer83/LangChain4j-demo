package com.lulala.langchain4j.agentic.a2a.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulala.langchain4j.agentic.a2a.server.service.CreativeWriterAgent;
import io.a2a.spec.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A2A JSON-RPC 控制器
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 16:55
 */
@RestController
@RequestMapping("/a2a/server")
public class A2AJsonRpcController {

    private final CreativeWriterAgent storyWriterService;
    private final AgentCard agentCardOfStoryWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public A2AJsonRpcController(CreativeWriterAgent storyWriterService, AgentCard agentCardOfStoryWriter) {
        this.storyWriterService = storyWriterService;
        this.agentCardOfStoryWriter = agentCardOfStoryWriter;
    }

    /**
     * A2A 协议入口：接收 JSON-RPC 请求
     */
    @PostMapping
    public JSONRPCResponse<?> handleA2ARequest(@RequestBody JsonNode request) {
        Object id = requestId(request);
        String method = request.path("method").asText(null);
        try {
            // 根据 method 分发处理
            switch (method) {
                case "agent/card":
                case "agent/getAuthenticatedExtendedCard":
                    return handleAgentCard(id);
                case SendMessageRequest.METHOD:
                    return handleMessageSend(request);
                default:
                    return new JSONRPCErrorResponse(id, new MethodNotFoundError(null, "Unsupported method: " + method, null));
            }
        } catch (IllegalArgumentException e) {
            return new JSONRPCErrorResponse(id, new InvalidParamsError(e.getMessage()));
        } catch (Exception e) {
            return new JSONRPCErrorResponse(id, new io.a2a.spec.InternalError(e.getMessage()));
        }
    }

    /**
     * 处理 agent/card 请求 —— 返回 AgentCard
     */
    private JSONRPCResponse<?> handleAgentCard(Object id) {
        return new GetAuthenticatedExtendedCardResponse(id, agentCardOfStoryWriter);
    }

    /**
     * 处理 message/send 请求 —— 执行任务并返回结果
     */
    private JSONRPCResponse<?> handleMessageSend(JsonNode request) throws Exception {
        SendMessageRequest sendMessageRequest = objectMapper.treeToValue(request, SendMessageRequest.class);
        Message message = sendMessageRequest.getParams().message();

        // 提取用户输入文本
        String userText = extractTextFromMessage(message);

        // 调用 LangChain4j Agent 生成故事
        String story = storyWriterService.writeStory(userText);

        // 构建响应
        String taskId = taskId(message);
        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .parts(new TextPart(story))
                .build();
        Task task = new Task.Builder()
                .id(taskId)
                .contextId(contextId(message, taskId))
                .status(new TaskStatus(TaskState.COMPLETED))
                .history(List.of(message))
                .artifacts(List.of(artifact))
                .build();

        return new SendMessageResponse(sendMessageRequest.getId(), task);
    }

    private String extractTextFromMessage(Message message) {
        StringBuilder textBuilder = new StringBuilder();
        if (message.getParts() != null) {
            for (Part<?> part : message.getParts()) {
                if (part instanceof TextPart textPart) {
                    textBuilder.append(textPart.getText());
                }
            }
        }
        return textBuilder.toString();
    }

    private Object requestId(JsonNode request) {
        JsonNode id = request.get("id");
        return id == null || id.isNull() ? null : objectMapper.convertValue(id, Object.class);
    }

    private String taskId(Message message) {
        return message.getTaskId() == null || message.getTaskId().isBlank()
                ? UUID.randomUUID().toString()
                : message.getTaskId();
    }

    private String contextId(Message message, String defaultContextId) {
        return message.getContextId() == null || message.getContextId().isBlank()
                ? defaultContextId
                : message.getContextId();
    }
}
