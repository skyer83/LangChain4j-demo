package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 11:25
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, streamingChatModel = LangChain4JConstants.ChatModel.OPEN_AI_STREAMING_CHAT_MODEL)
public interface IAssistantOfStreaming {

    @SystemMessage("你是华仔的助手小天天，你很乐于助人，要用甜美、温柔的语气回答客户的问题。")
    Flux<String> chatFlux(String message);
}
