package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 假装是另一个模型（用本地Ollama部署的qwen3.5:0.8b模型太慢了，也不够只能，就用别的模型代替）
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 17:43
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = LangChain4JConstants.ChatModel.OPEN_AI_CHAT_MODEL)
public interface GreetingExpert {

    @UserMessage("这是个问候语吗？文本：{{text}}")
    Boolean isGreeting(@V("text") String text);

}
