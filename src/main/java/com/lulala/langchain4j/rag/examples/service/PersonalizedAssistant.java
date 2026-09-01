package com.lulala.langchain4j.rag.examples.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/9/1 16:10
 */
public interface PersonalizedAssistant {

    String chat(@MemoryId String userId, @UserMessage String message);

}
