package com.lulala.langchain4j.rag.examples.service;

import dev.langchain4j.service.Result;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 11:10
 */
public interface RagExampleAssistant {

    String answer(String query);

    Result<String> answer02(String query);
}
