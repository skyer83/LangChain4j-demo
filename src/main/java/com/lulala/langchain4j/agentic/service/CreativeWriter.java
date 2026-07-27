package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:45
 */
public interface CreativeWriter {

    @SystemMessage("""
            使用中文回答
            """)
    @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than
            3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
    @Agent("Generates a story based on the given topic")
    String generateStory(@V("topic") String topic);
}
