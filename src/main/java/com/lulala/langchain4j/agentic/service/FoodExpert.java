package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 10:17
 */
public interface FoodExpert {
    @UserMessage("""
        你是一位出色的晚间活动规划师。
        请根据给定的情绪 {{mood}}，推荐3道匹配的餐食。
        对于每道餐食，只需提供餐食名称。
        仅输出包含这3个项目的列表，不要包含任何其他内容。
        """)
    @Agent
    List<String> findMeal(@V("mood") String mood);
}
