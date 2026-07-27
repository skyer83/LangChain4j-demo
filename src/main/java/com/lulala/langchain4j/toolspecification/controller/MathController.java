package com.lulala.langchain4j.toolspecification.controller;

import cn.hutool.json.JSONUtil;
import com.lulala.langchain4j.toolspecification.tools.MathTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 14:24
 */
@Slf4j
@RestController
@RequestMapping("/math")
public class MathController {

    @Autowired
    private ChatModel deepseekChatModel;

    /**
     * 两数相加
     * @param a
     * @param b
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/27 14:34
     */
    @RequestMapping("/sum")
    public String sum(@RequestParam("a") double a, @RequestParam("b") double b) {
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(MathTools.class)
                .stream()
                .filter(toolSpecification -> "sum".equals(toolSpecification.name()))
                .toList();

        UserMessage userMessage = UserMessage.from("求数字 %.2f 和 %.2f 的和。".formatted(a, b));
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse chatResponse = deepseekChatModel.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info(">>>>>> aiMessage: {}", aiMessage);
        log.info(">>>>>> toolExecutionRequests: {}", aiMessage.toolExecutionRequests());

        if (!aiMessage.hasToolExecutionRequests()) {
            throw new IllegalStateException("模型没有返回工具调用请求，MathTool.sum 未被调用。模型响应：" + aiMessage.text());
        }

        MathTools mathTool = new MathTools();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(userMessage);
        messages.add(aiMessage);
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            String result = new DefaultToolExecutor(mathTool, toolExecutionRequest).execute(toolExecutionRequest, null);
            log.info(">>>>>> toolExecutionRequest: {}", toolExecutionRequest);
            log.info(">>>>>> toolExecutionResult: {}", result);
            messages.add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
        }

        ChatRequest finalChatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        ChatResponse finalChatResponse = deepseekChatModel.chat(finalChatRequest);
        AiMessage finalAiMessage = finalChatResponse.aiMessage();
        log.info(">>>>>> finalAiMessage: {}", finalAiMessage);
        return finalAiMessage.text();
    }

    /**
     * 开平方
     * @param a
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/27 14:34
     */
    @RequestMapping("/squareRoot")
    public String squareRoot(@RequestParam("a") double a) {
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(MathTools.class)
                .stream()
                .filter(toolSpecification -> "squareRoot".equals(toolSpecification.name()))
                .toList();

        UserMessage userMessage = UserMessage.from("求数字 %.4f 的平方根。".formatted(a));
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse chatResponse = deepseekChatModel.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info(">>>>>> aiMessage: {}", JSONUtil.toJsonStr(aiMessage));
        log.info(">>>>>> toolExecutionRequests: {}", aiMessage.toolExecutionRequests());

        if (!aiMessage.hasToolExecutionRequests()) {
            throw new IllegalStateException("模型没有返回工具调用请求，MathTool.squareRoot 未被调用。模型响应：" + aiMessage.text());
        }

        MathTools mathTool = new MathTools();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(userMessage);
        messages.add(aiMessage);
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            // 前面模型判断需要调用哪些工具
            // 开始执行 MathTool.squareRoot 方法
            String result = new DefaultToolExecutor(mathTool, toolExecutionRequest).execute(toolExecutionRequest, null);
            // >>>>>> toolExecutionRequest: ToolExecutionRequest { id = "call_00_VPlsGPyi5AqPHwWlg4C32463", name = "squareRoot", arguments = "{"a": 475695037565}" }
            log.info(">>>>>> toolExecutionRequest: {}", toolExecutionRequest);
            // >>>>>> toolExecutionResult: 689706.4865324959
            log.info(">>>>>> toolExecutionResult: {}", result);
            messages.add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
        }

        ChatRequest finalChatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        ChatResponse finalChatResponse = deepseekChatModel.chat(finalChatRequest);
        AiMessage finalAiMessage = finalChatResponse.aiMessage();
        log.info(">>>>>> finalAiMessage: {}", finalAiMessage);
        return finalAiMessage.text();
    }
}
