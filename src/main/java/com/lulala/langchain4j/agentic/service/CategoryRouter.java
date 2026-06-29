package com.lulala.langchain4j.agentic.service;

import com.lulala.langchain4j.agentic.enums.RequestCategory;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 11:42
 */
public interface CategoryRouter {
    @UserMessage("""
        分析以下用户请求，并将其归类为‘法律（legal）’、‘医疗（medical）’或‘技术（technical）’。
        如果该请求不属于上述任何类别，请将其归类为‘未知（unknown）’。
        仅回复这四个词中的一个，不要包含任何其他内容。
        用户请求为：‘{{request}}’。
        """)
    @Agent("对用户请求进行分类")
    RequestCategory classify(@V("request") String request);
}
