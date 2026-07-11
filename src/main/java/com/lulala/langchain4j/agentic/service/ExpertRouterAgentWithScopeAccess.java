package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

/**
 * 支持访问 AgenticScope 注册表的根代理。
 * @author shenjh
 * @version 1.0
 * @since 2026/7/6 20:30
 */
public interface ExpertRouterAgentWithScopeAccess extends AgenticScopeAccess {

    @Agent
    String ask(@MemoryId String memoryId, @V("request") String request);
}
