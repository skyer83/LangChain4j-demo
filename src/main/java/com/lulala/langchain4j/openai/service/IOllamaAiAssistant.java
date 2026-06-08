package com.lulala.langchain4j.openai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 13:40
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "ollamaChatModel")
public interface IOllamaAiAssistant {

    @SystemMessage("你叫ollamaChatModel，是一个有礼貌的助手")
    String chat(String userMessage);

}
