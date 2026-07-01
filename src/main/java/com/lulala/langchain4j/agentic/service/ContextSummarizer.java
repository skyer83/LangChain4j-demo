package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/1 21:58
 */
public interface ContextSummarizer {

    @UserMessage("""
            请为以下 AI 代理与用户的对话写一个非常简短的摘要，最多不超过两句话。
            用户的对话内容是：'{{context}}'。
            """)
    @Agent
    String summarize(@V("context") String context);
}
