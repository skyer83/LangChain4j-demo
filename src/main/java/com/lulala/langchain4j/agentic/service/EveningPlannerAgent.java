package com.lulala.langchain4j.agentic.service;

import com.lulala.langchain4j.agentic.domain.vo.EveningPlan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 10:26
 */
public interface EveningPlannerAgent {

    @Agent
    List<EveningPlan> plan(@V("mood") String mood);
}
