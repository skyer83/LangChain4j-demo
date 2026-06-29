package com.lulala.langchain4j.agentic.service;

import com.lulala.langchain4j.agentic.spring.AgenticAiService;
import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/10 15:08
 */
@AgenticAiService(chatModel = LangChain4JConstants.ChatModel.GPT_CHAT_MODEL)
public interface StyleEditorZh {

    @UserMessage("""
        你是一位专业编辑。
        请分析并重写以下故事，使其更好地契合 {{style}} 的写作风格。
        除了故事本身，不要返回任何其他内容。该故事为“{{story}}”。
        """)
    @Agent(outputKey = "story", description = "对故事进行编辑，使其更好地契合给定的写作风格。")
    String editStory(@V("story") String story, @V("style") String style);

}
