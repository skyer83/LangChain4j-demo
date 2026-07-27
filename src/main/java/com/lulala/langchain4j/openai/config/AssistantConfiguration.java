package com.lulala.langchain4j.openai.config;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import com.lulala.langchain4j.openai.listener.MyChatModelListener;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * 自定义调用的大模型
     * @param httpClientBuilder
     * @param properties
     * @param chatModelListeners
     * @return dev.langchain4j.model.chat.ChatModel
     * @author shenjh
     * @since 2026/7/16 9:43
     */
    @Bean(LangChain4JConstants.ChatModel.DEEPSEEK_CHAT_MODEL)
    ChatModel deepseekChatModel(
            @Qualifier("openAiChatModelHttpClientBuilder") HttpClientBuilder httpClientBuilder,
            DeepseekChatModelProperties properties,
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
}
