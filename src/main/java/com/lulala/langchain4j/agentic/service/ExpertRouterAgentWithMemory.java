package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 11:57
 */
public interface ExpertRouterAgentWithMemory {
    @Agent
    String ask(@MemoryId String memoryId, @V("request") String request);
}
