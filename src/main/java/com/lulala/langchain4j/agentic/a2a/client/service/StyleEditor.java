package com.lulala.langchain4j.agentic.a2a.client.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 本地代理：根据给定风格编辑故事
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 17:25
 */
public interface StyleEditor {

    @Agent("根据给定的风格编辑故事，使其更符合该风格")
    String editStory(@V("story") String story, @V("style") String style);
}
