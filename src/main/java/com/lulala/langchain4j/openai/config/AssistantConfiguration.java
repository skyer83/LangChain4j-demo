package com.lulala.langchain4j.openai.config;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import com.lulala.langchain4j.openai.listener.MyChatModelListener;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 10:23
 */
@Configuration
public class AssistantConfiguration {

    /**
     * 为 ChatModel 或 StreamingChatModel 启用可观测性，参见：https://langchain4j.cn/tutorials/spring-boot-integration.html#可观测性-observability
     * <pre>
     *     所有在应用上下文中的 ChatModelListener Bean 都会被自动注入到由 Spring Boot Starter 创建的 ChatModel 与 StreamingChatModel 中
     * </pre>
     * @return dev.langchain4j.model.chat.listener.ChatModelListener
     * @author shenjh
     * @since 2026/6/9 10:01
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new MyChatModelListener();
    }

    @Bean
    @ConfigurationProperties(prefix = "langchain4j.custom.chat-models.gpt")
    GptChatModelProperties gptChatModelProperties() {
        return new GptChatModelProperties();
    }
    
    /**
     * 自定义调用的大模型
     * @param httpClientBuilder
     * @param properties
     * @param chatModelListeners
     * @return dev.langchain4j.model.chat.ChatModel 
     * @author shenjh
     * @since 2026/7/16 9:43
     */
    @Bean(LangChain4JConstants.ChatModel.GPT_CHAT_MODEL)
    ChatModel gptChatModel(
            @Qualifier("openAiChatModelHttpClientBuilder") HttpClientBuilder httpClientBuilder,
            GptChatModelProperties properties,
            ObjectProvider<ChatModelListener> chatModelListeners) {
        return OpenAiChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .logRequests(properties.getLogRequests())
                .logResponses(properties.getLogResponses())
                .listeners(chatModelListeners.orderedStream().toList())
                .build();
    }

    /**
     * 可参考 dev.langchain4j.openai.spring.ChatModelProperties 配置项进行配置
     * @author shenjh
     * @since 2026/7/16 11:04
     * @version 1.0
     */
    static class GptChatModelProperties {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Boolean logRequests;
        private Boolean logResponses;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Boolean getLogRequests() {
            return logRequests;
        }

        public void setLogRequests(Boolean logRequests) {
            this.logRequests = logRequests;
        }

        public Boolean getLogResponses() {
            return logResponses;
        }

        public void setLogResponses(Boolean logResponses) {
            this.logResponses = logResponses;
        }
    }
}
