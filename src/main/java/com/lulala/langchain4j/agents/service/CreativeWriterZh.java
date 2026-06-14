package com.lulala.langchain4j.agents.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:45
 */
public interface CreativeWriterZh {
    @UserMessage("""
            你是一位创意作家。
            请围绕给定的主题，写一段不超过三句话的故事草稿。
            除了故事本身，不要返回任何其他内容。该主题为 {{topic}}。
            """)
    @Agent(outputKey = "story", description = "根据给定的主题创作一个故事。")
    String generateStory(@V("topic") String topic);
}
