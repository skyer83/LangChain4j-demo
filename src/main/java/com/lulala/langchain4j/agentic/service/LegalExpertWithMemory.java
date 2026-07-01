package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 11:48
 */
public interface LegalExpertWithMemory {
    @UserMessage("""
        你是一位法律专家。
        请从法律的角度分析以下用户请求，并提供尽可能最佳的解答。
        用户请求为：{{request}}。
        """)
    @Agent(value = "法律专家", outputKey = "response")
    String legal(@MemoryId String memoryId, @V("request") String request);
}
