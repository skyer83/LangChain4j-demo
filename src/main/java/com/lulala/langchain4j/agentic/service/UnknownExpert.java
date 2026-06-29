package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 11:48
 */
public interface UnknownExpert {
    @UserMessage("""
        你是一位客服。
        你只要严格回答：对不起，我无法确认您的问题，请提供更详细的描述。
        不要回答别的内容。
        """)
    @Agent
    String unknown();
}
