package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.enums.TemperatureUnit;
import com.lulala.langchain4j.toolspecification.tools.WeatherTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 参见：https://langchain4j.cn/tutorials/tools.html<br/>
 * 低级工具 API
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 14:24
 */
@Slf4j
@RestController
@RequestMapping("/weatherForcast")
public class WeatherForecastController {

    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private StreamingChatModel deepseekStreamingChatModel;

    /**
     * 获取指定城市的天气预报
     * @param city
     * @param temperatureUnit
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/27 14:34
     */
    @RequestMapping("/getWeather01")
    public String getWeather01(@RequestParam("city") String city, @RequestParam("temperatureUnit") TemperatureUnit temperatureUnit) {
        List<ToolSpecification> toolSpecifications = getToolSpecification01();
        return getWeater(city, temperatureUnit, toolSpecifications);
    }

    private List<ToolSpecification> getToolSpecification01() {
        return List.of(ToolSpecification.builder()
                .name("getWeather")
                .description("返回给定城市的天气预报")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "要返回天气预报的城市")
                        .addEnumProperty("temperatureUnit", List.of(TemperatureUnit.CELSIUS.name(), TemperatureUnit.FAHRENHEIT.name()))
                        .required("city")
                        .build())
                .build());
    }

    private String getWeater(String city, TemperatureUnit temperatureUnit, List<ToolSpecification> toolSpecifications) {
        UserMessage userMessage = UserMessage.from("%s明天的天气有多少%s".formatted(city, temperatureUnit));
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse chatResponse = deepseekChatModel.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info(">>>>>> aiMessage: {}", aiMessage);
        log.info(">>>>>> toolExecutionRequests: {}", aiMessage.toolExecutionRequests());

        if (!aiMessage.hasToolExecutionRequests()) {
            throw new RuntimeException("模型没有返回工具调用请求，WeatherTools.getWeather 未被调用。模型响应：" + aiMessage.text());
        }

        WeatherTools weatherTools = new WeatherTools();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(userMessage);
        messages.add(aiMessage);
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            String result = new DefaultToolExecutor(weatherTools, toolExecutionRequest).execute(toolExecutionRequest, null);
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
     * 获取指定城市的天气预报
     * @param city
     * @param temperatureUnit
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/27 14:34
     */
    @RequestMapping("/getWeather02")
    public String getWeather02(@RequestParam("city") String city, @RequestParam("temperatureUnit") TemperatureUnit temperatureUnit) {
        List<ToolSpecification> toolSpecifications = getToolSpecification02();
        return getWeater(city, temperatureUnit, toolSpecifications);
    }

    private List<ToolSpecification> getToolSpecification02() {
        WeatherTools weatherTools = new WeatherTools();
        return ToolSpecifications.toolSpecificationsFrom(weatherTools);
    }

    /**
     * 获取指定城市的天气预报
     * @param city
     * @param temperatureUnit
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/27 14:34
     */
    @RequestMapping("/getWeatherStreaming")
    public SseEmitter getWeatherStreaming(@RequestParam("city") String city, @RequestParam("temperatureUnit") TemperatureUnit temperatureUnit) {
        List<ToolSpecification> toolSpecifications = getToolSpecification02();

        SseEmitter emitter = new SseEmitter(0L);

        UserMessage userMessage = UserMessage.from("%s明天的天气有多少%s".formatted(city, temperatureUnit));
        ChatRequest chatRequest = ChatRequest.builder().messages(userMessage).toolSpecifications(toolSpecifications).build();
        deepseekStreamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // StreamingChatResponseHandler.super.onPartialResponse(partialResponse);
                System.out.println("onPartialResponse01: " + partialResponse);
                try {
                    emitter.send(SseEmitter.event().data(partialResponse));
                } catch (Exception e) {
                    log.error("[streamingChat] SSE发送失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                System.out.println("onPartialResponse02: PartialResponse: " + partialResponse);
                System.out.println("onPartialResponse02: PartialResponseContext: " + context);
                StreamingChatResponseHandler.super.onPartialResponse(partialResponse, context);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                System.out.println("onPartialThinking01: " + partialThinking);
                StreamingChatResponseHandler.super.onPartialThinking(partialThinking);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
                System.out.println("onPartialThinking02: PartialThinking: " + partialThinking);
                System.out.println("onPartialThinking02: PartialThinkingContext: " + context);
                StreamingChatResponseHandler.super.onPartialThinking(partialThinking, context);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                // StreamingChatResponseHandler.super.onPartialToolCall(partialToolCall);
                System.out.println("onPartialToolCall01: " + partialToolCall);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext context) {
                System.out.println("onPartialToolCall02: PartialToolCall: " + partialToolCall);
                System.out.println("onPartialToolCall02: PartialToolCallContext: " + context);
                StreamingChatResponseHandler.super.onPartialToolCall(partialToolCall, context);
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                // StreamingChatResponseHandler.super.onCompleteToolCall(completeToolCall);
                System.out.println("onCompleteToolCall: " + completeToolCall);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("onCompleteResponse: " + completeResponse);
                AiMessage aiMessage = completeResponse.aiMessage();
                if (!aiMessage.hasToolExecutionRequests()) {
                    emitter.complete();
                    return;
                }

                try {
                    WeatherTools weatherTools = new WeatherTools();
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(userMessage);
                    messages.add(aiMessage);
                    for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                        String result = new DefaultToolExecutor(weatherTools, toolExecutionRequest).execute(toolExecutionRequest, null);
                        log.info(">>>>>> streaming toolExecutionRequest: {}", toolExecutionRequest);
                        log.info(">>>>>> streaming toolExecutionResult: {}", result);
                        messages.add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
                    }

                    ChatRequest finalChatRequest = ChatRequest.builder()
                            .messages(messages)
                            .build();
                    streamFinalWeatherResponse(finalChatRequest, emitter);
                } catch (Exception e) {
                    log.error("[getWeatherStreaming] 工具执行或最终响应失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("onError: {}", error.getMessage(), error);
                emitter.completeWithError(error);
            }
        });
        return emitter;
    }

    private void streamFinalWeatherResponse(ChatRequest finalChatRequest, SseEmitter emitter) {
        deepseekStreamingChatModel.chat(finalChatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                log.info("[getWeatherStreaming] final partialResponse: {}", partialResponse);
                try {
                    emitter.send(SseEmitter.event().data(partialResponse));
                } catch (Exception e) {
                    log.error("[getWeatherStreaming] SSE发送最终响应失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                log.info("[getWeatherStreaming] final completeResponse: {}", completeResponse);
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[getWeatherStreaming] 最终流式响应异常: {}", error.getMessage(), error);
                emitter.completeWithError(error);
            }
        });
    }
}
