package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 11:57
 */
public interface ExpertRouterAgent {
    @Agent
    String ask(@V("request") String request);
}
