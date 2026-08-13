package com.lulala.langchain4j.openai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 可参考 dev.langchain4j.openai.spring.ChatModelProperties 配置项进行配置
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 10:52
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.custom.streaming-chat-model.deepseek")
public class DeepseekStreamingChatModelProperties {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean logRequests;
    private Boolean logResponses;
}
