package com.lulala.langchain4j.toolspecification.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulala.langchain4j.toolspecification.enums.TemperatureUnit;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 21:21
 */
@Slf4j
public class WeatherTools {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, String> CITY_SEARCH_ALIASES = Map.of(
            "北京", "Beijing",
            "北京市", "Beijing",
            "上海", "Shanghai",
            "上海市", "Shanghai",
            "广州", "Guangzhou",
            "广州市", "Guangzhou",
            "深圳", "Shenzhen",
            "深圳市", "Shenzhen",
            "厦门", "Xiamen",
            "厦门市", "Xiamen"
    );
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WeatherTools() {}

    @Tool("返回给定城市的天气预报")
    public String getWeather(@P("要返回天气预报的城市名称") String city,
                             @P("温度单位，中文天气问题默认使用 CELSIUS") TemperatureUnit temperatureUnit) {
        try {
            TemperatureUnit unit = temperatureUnit == null ? TemperatureUnit.CELSIUS : temperatureUnit;
            log.info("调用 WeatherTools.getWeather，city: {}, temperatureUnit: {}", city, unit);
            JsonNode location = resolveLocation(city);
            if (location == null) {
                return "无法确定城市 %s 的经纬度".formatted(city);
            }

            double latitude = location.path("latitude").asDouble();
            double longitude = location.path("longitude").asDouble();
            String resolvedCity = location.path("name").asText(city);
            LocalDate tomorrow = LocalDate.now(DEFAULT_ZONE_ID).plusDays(1);

            URI forecastUri = URI.create("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%%2FShanghai&forecast_days=3"
                    .formatted(latitude, longitude));
            JsonNode forecast = getJson(forecastUri);
            JsonNode dates = forecast.path("daily").path("time");
            JsonNode maxTemperatures = forecast.path("daily").path("temperature_2m_max");
            JsonNode minTemperatures = forecast.path("daily").path("temperature_2m_min");

            for (int i = 0; i < dates.size(); i++) {
                if (tomorrow.toString().equals(dates.get(i).asText())) {
                    double min = convertFromCelsius(minTemperatures.get(i).asDouble(), unit);
                    double max = convertFromCelsius(maxTemperatures.get(i).asDouble(), unit);
                    String unitText = unitText(unit);
                    return "%s 明天（%s）气温约 %.1f%s 到 %.1f%s".formatted(resolvedCity, tomorrow, min, unitText, max, unitText);
                }
            }
            return "无法确定 %s 明天（%s）的天气预报".formatted(resolvedCity, tomorrow);
        } catch (Exception e) {
            log.error("查询天气失败", e);
            return "查询天气失败：" + e.getMessage();
        }
    }

    private JsonNode resolveLocation(String city) throws Exception {
        String searchName = CITY_SEARCH_ALIASES.getOrDefault(city, city);
        String encodedCity = URLEncoder.encode(searchName, StandardCharsets.UTF_8);
        URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh&format=json"
                .formatted(encodedCity));
        JsonNode results = getJson(uri).path("results");
        return results.isArray() && !results.isEmpty() ? results.get(0) : null;
    }

    private JsonNode getJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + uri);
        }
        return OBJECT_MAPPER.readTree(response.body());
    }

    private double convertFromCelsius(double celsius, TemperatureUnit temperatureUnit) {
        return switch (temperatureUnit) {
            case FAHRENHEIT -> celsius * 9 / 5 + 32;
            case KELVIN -> celsius + 273.15;
            case CELSIUS -> celsius;
        };
    }

    private String unitText(TemperatureUnit temperatureUnit) {
        return switch (temperatureUnit) {
            case FAHRENHEIT -> "°F";
            case KELVIN -> "K";
            case CELSIUS -> "°C";
        };
    }
}
