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
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
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
            throw new IllegalStateException("模型没有返回工具调用请求，WeatherTools.getWeather 未被调用。模型响应：" + aiMessage.text());
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

    private List<ToolSpecification> getToolSpecification02() {
        return ToolSpecifications.toolSpecificationsFrom(WeatherTools.class);
    }
}
