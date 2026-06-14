package com.lulala.langchain4j.agents.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/10 15:08
 */
public interface StyleEditor {

    @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the given writing style of {{style}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
    @Agent("Edits a story to better fit a given writing style")
    String editStory(@V("story") String story, @V("style") String style);

}
