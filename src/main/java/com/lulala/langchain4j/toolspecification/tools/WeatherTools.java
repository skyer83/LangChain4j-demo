package com.lulala.langchain4j.toolspecification.tools;

import com.lulala.langchain4j.toolspecification.enums.TemperatureUnit;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 21:21
 */
public class WeatherTools {

    private ChatModel chatModel;

    public WeatherTools(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Tool("返回给定城市的天气预报")
    public String getWeather(@P("要返回天气预报的城市名称") String city, TemperatureUnit temperatureUnit) {
        String formatted = "明天城市 %s 天气温度为多少 %s？要求严格按'明天城市 %s 为 xxx %s'的格式返回，如果无法确定则返回'无法确定'".formatted(city, temperatureUnit, city, temperatureUnit);
        return chatModel.chat(formatted);
    }
}
