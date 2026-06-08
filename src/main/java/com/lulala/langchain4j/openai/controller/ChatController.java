package com.lulala.langchain4j.openai.controller;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 10:32
 */
@RestController
@RequestMapping("/openai")
public class ChatController {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public ChatController(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatModel.chat(message);
    }

//    @GetMapping("/streamingChat")
//    public SseEmitter streamingChat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
//        SseEmitter emitter = new SseEmitter(0L);
//        streamingChatModel.chat(message, new StreamingChatResponseHandler() {
//            @Override
//            public void onPartialResponse(String token) {
//                try {
//                    emitter.send(SseEmitter.event().data(token));
//                } catch (Exception e) {
//                    emitter.completeWithError(e);
//                }
//            }
//
//            @Override
//            public void onCompleteResponse(ChatResponse response) {
//                emitter.complete();
//            }
//
//            @Override
//            public void onError(Throwable error) {
//                emitter.completeWithError(error);
//            }
//        });
//
//        return emitter;
//    }
}
