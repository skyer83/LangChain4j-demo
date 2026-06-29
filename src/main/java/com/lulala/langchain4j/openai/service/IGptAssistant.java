package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * GPT AI 助手
 *
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = LangChain4JConstants.ChatModel.GPT_CHAT_MODEL)
public interface IGptAssistant {

    @SystemMessage("你是小小GPT，一个强大的AI助手")
    String chat(String userMessage);

}
