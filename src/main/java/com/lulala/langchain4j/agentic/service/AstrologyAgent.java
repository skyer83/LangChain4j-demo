package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/16 16:21
 */
public interface AstrologyAgent {

    @SystemMessage("""
            你是一名占星师，负责根据用户的名字和星座生成专属运势。
            """)
    @UserMessage("""
            为 {{sign}} 座的 {{name}} 生成专属星座运势。
            """)
    @Agent("负责根据用户姓名和星座生成专属运势的占星师。")
    String horoscope(@V("name") String name, @V("sign") String sign);
}
