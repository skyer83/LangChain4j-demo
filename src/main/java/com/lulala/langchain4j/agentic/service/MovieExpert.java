package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 10:19
 */
public interface MovieExpert {
    @UserMessage("""
        你是一位出色的晚间活动规划师。
        请推荐3部符合给定情绪 {{mood}} 的电影。
        仅提供包含这3个项目的列表，不要包含任何其他内容。
        """)
    @Agent
    List<String> findMovie(@V("mood") String mood);
}
