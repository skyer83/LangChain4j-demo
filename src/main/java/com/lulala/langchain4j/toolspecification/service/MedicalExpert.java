package com.lulala.langchain4j.toolspecification.service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/20 17:41
 */
public interface MedicalExpert {

    /**
     * 医疗问题
     * @param request
     * @return
     */
    @UserMessage("""
            您是一位医学专家。
            请从医学角度分析以下用户请求，并提供尽可能准确的解答。
            用户请求是：{{request}}。
            """)
    @Tool("你是位医学专家")
    String medicalRequest(@V("request") String request);
}
