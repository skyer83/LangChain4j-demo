package com.lulala.langchain4j.openai.config;

import com.lulala.langchain4j.openai.listener.MyChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelListener;
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
}
