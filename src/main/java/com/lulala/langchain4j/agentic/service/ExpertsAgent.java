package com.lulala.langchain4j.agentic.service;

import com.lulala.langchain4j.agentic.enums.RequestCategory;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/1 20:42
 */
public interface ExpertsAgent {

    @ConditionalAgent(outputKey = "response", subAgents = {
            MedicalExpert.class,
            TechnicalExpert.class,
            LegalExpert.class,
            UnknownExpert.class
    })
    String askExpert(@V("request") String request);

    @ActivationCondition(MedicalExpert.class)
    static boolean activateMedical(@V("category") RequestCategory category) {
        return category == RequestCategory.MEDICAL;
    }

    @ActivationCondition(TechnicalExpert.class)
    static boolean activateTechnical(@V("category") RequestCategory category) {
        return category == RequestCategory.TECHNICAL;
    }

    @ActivationCondition(LegalExpert.class)
    static boolean activateLegal(@V("category") RequestCategory category) {
        return category == RequestCategory.LEGAL;
    }

    @ActivationCondition(UnknownExpert.class)
    static boolean activateUnknown(@V("category") RequestCategory category) {
        return category == null || category == RequestCategory.UNKNOWN;
    }
}
