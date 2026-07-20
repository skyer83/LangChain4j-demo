package com.lulala.langchain4j.agentic.a2a.server.controller;

import io.a2a.spec.AgentCard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A2A AgentCard discovery endpoint.
 * @author shenjh
 * @version 1.0
 * @since 2026/7/20 18:06
 */
@RestController
public class A2AAgentCardController {

    private final AgentCard agentCardOfStoryWriter;

    public A2AAgentCardController(AgentCard agentCardOfStoryWriter) {
        this.agentCardOfStoryWriter = agentCardOfStoryWriter;
    }
    
    /**
     * 说明本智能体支持具备哪些能力，形成的地址只能是：域名 + 端口 + /.well-known/agent-card.json，中间不能穿插其他路径，
     * 地址拼接参见 io.a2a.client.http.A2ACardResolver，如：http://localhost:18081/.well-known/agent-card.json
     * @return io.a2a.spec.AgentCard 
     * @author shenjh
     * @since 2026/7/20 10:20
     */
    @GetMapping("/.well-known/agent-card.json")
    public AgentCard agentCard() {
        return agentCardOfStoryWriter;
    }
}
