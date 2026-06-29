package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 17:46
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = LangChain4JConstants.ChatModel.OPEN_AI_CHAT_MODEL)
public interface ChatBot {

    @SystemMessage("你是一位来自“Miles of Smiles”公司的礼貌聊天机器人。")
    String chat(String userMessage);
}
