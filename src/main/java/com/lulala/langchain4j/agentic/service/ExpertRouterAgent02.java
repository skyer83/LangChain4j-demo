package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

/**
 * 声明式 API 写法
 * @author shenjh
 * @version 1.0
 * @since 2026/7/1 21:11
 */
public interface ExpertRouterAgent02 {

    @SequenceAgent(outputKey = "response", subAgents = {
            CategoryRouter.class,
            ExpertsAgent.class
    })
    String ask(@V("request") String request);
}
