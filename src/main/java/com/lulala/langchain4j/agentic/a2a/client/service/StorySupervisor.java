package com.lulala.langchain4j.agentic.a2a.client.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 监督代理：协调远程写作代理和本地编辑代理
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 17:26
 */
public interface StorySupervisor {

    @Agent("根据主题和风格创作一个完整的故事")
    String createStyledStory(@V("topic") String topic, @V("style") String style);
}
