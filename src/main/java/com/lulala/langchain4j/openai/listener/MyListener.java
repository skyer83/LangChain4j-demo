package com.lulala.langchain4j.openai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 9:57
 */
@Configuration
public class MyListener {
    
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
        return new ChatModelListener() {

            private static final Logger logger = LoggerFactory.getLogger(ChatModelListener.class);

            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                logger.info("onRequest(): {}", requestContext.chatRequest());
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                logger.info("onResponse(): {}", responseContext.chatResponse());
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                logger.info("onError(): {}", errorContext.error().getMessage());
            }
        };
    }
}
