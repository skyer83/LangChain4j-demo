package com.lulala.langchain4j.agentic.a2a.client.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 远程 A2A 代理的本地接口映射（类型安全）
 * 对应服务端 http://localhost:8080 上的 CreativeWriterAgent
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 17:26
 */
public interface A2ACreativeWriter {

    @Agent(outputKey = "story", description = "根据主题生成创意故事")
    String generateStory(@V("topic") String topic);
}
