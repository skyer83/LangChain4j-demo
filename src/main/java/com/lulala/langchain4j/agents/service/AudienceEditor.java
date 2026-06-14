package com.lulala.langchain4j.agents.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:48
 */
public interface AudienceEditor {

    @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the target audience of {{audience}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
    @Agent(description = "Edits a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
}
