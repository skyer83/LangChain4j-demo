package com.lulala.langchain4j.agentic.a2a.client.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 本地代理：根据给定风格编辑故事
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 17:25
 */
public interface StyleEditor {

    @UserMessage("""
            你是一位专业的故事编辑。
            请分析并重写以下故事，使其更好地契合 {{style}} 风格。
            除了改写后的故事本身，不要返回任何其他内容。
            故事：{{story}}
            """)
    @Agent(outputKey = "story", description = "根据给定的风格编辑故事，使其更符合该风格")
    String editStory(@V("story") String story, @V("style") String style);
}
