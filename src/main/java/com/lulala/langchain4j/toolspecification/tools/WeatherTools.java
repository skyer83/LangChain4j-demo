package com.lulala.langchain4j.toolspecification.tools;

import com.lulala.langchain4j.toolspecification.enums.TemperatureUnit;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 21:21
 */
public class WeatherTools {

    @Tool("返回给定城市的天气预报")
    public String getWeather(@P("要返回天气预报的城市名称") String city, TemperatureUnit temperatureUnit) {
        return "明天城市 %s 为 28 %s".formatted(city, temperatureUnit);
    }
}
