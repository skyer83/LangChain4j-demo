package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/16 11:47
 */
public interface MySupervisorAgent01 {

    @Agent
    String invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);
}
