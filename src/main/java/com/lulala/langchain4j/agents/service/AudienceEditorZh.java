package com.lulala.langchain4j.agents.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:48
 */
public interface AudienceEditorZh {

    @UserMessage("""
        你是一位专业编辑。
        请分析并重写以下故事，使其更好地契合目标受众 {{audience}}。
        除了故事本身，不要返回任何其他内容。
        该故事为“{{story}}”。
        """)
    @Agent(outputKey = "story", description = "Edits a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
}
