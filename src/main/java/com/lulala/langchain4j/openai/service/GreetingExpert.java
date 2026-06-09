package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 17:43
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = LangChain4JConstants.ChatModel.OLLAMA_CHAT_MODEL)
public interface GreetingExpert {

    @UserMessage("这是个问候语吗？文本：{{text}}")
    Boolean isGreeting(@V("text") String text);

}
