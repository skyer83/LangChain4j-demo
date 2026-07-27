package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 10:00
 */
public interface StyledWriter {

    @Agent
    String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}
