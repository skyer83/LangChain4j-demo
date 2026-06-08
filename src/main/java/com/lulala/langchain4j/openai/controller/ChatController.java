package com.lulala.langchain4j.openai.controller;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 参见：https://langchain4j.cn/tutorials/spring-boot-integration.html#spring-boot-starters
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 10:32
 */
@RestController
@RequestMapping("/openai")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public ChatController(@Qualifier("openAiChatModel") ChatModel chatModel,
                          StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatModel.chat(message);
    }

    @GetMapping("/streamingChat")
    public SseEmitter streamingChat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        log.info("[streamingChat] 收到请求, message: {}", message);
        SseEmitter emitter = new SseEmitter(0L);

        StringBuilder fullContent = new StringBuilder();
        streamingChatModel.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                fullContent.append(token);
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (Exception e) {
                    log.error("[streamingChat] SSE发送失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                log.info("[streamingChat] 流式响应完成, 完整内容: {}", fullContent);
                log.info("[streamingChat] token用量 - input: {}, output: {}",
                        response.tokenUsage().inputTokenCount(),
                        response.tokenUsage().outputTokenCount());
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[streamingChat] 流式响应异常, 已接收部分内容: {}", fullContent, error);
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }
}
