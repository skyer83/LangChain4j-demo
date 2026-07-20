package com.lulala.langchain4j.agentic.a2a.server.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Service;

/**
 * 创意写作 AI 服务
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 16:52
 */
@Service
public class CreativeWriterAgent {

    private final CreativeWriter writer;

    public CreativeWriterAgent(ChatModel gptChatModel) {
        // 创建 AI 服务
        this.writer = AiServices.builder(CreativeWriter.class)
                .chatModel(gptChatModel)
                .build();
    }

    public String writeStory(String topic) {
        return writer.writeStory(topic);
    }

    /** Agent 接口定义 */
    public interface CreativeWriter {
        @SystemMessage("""
                你是一位富有想象力的故事作家。
                根据用户给定的主题，创作一个结构完整、语言生动的短篇故事，约300字。
                除了故事本身，不要返回任何其他内容。
                """)
        @UserMessage("主题：{{topic}}")
        String writeStory(@V("topic") String topic);
    }
}
