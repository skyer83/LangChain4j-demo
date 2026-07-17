package com.lulala.langchain4j.agentic.a2a.server.config;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 16:47
 */
@Configuration
public class StoryAgentCardProducer {

    @Value("${server.port}")
    private int port;

    @Bean
    @PublicAgentCard
    public AgentCard agentCardOfStoryWriter() {
        return new AgentCard.Builder()
                .name("创意写作助手")
                .description("根据主题创作富有想象力的短篇故事。支持中文和英文创作，故事结构完整，语言生动。")
                // A2A 服务器地址
                .url("http://localhost:" + port)
                .version("1.0.0")
                // 能力声明
                .capabilities(new AgentCapabilities.Builder()
                        // 不支持流式输出
                        .streaming(false)
                        // 不支持推送通知
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                // 支持的输入/输出格式
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                // 技能列表
                .skills(List.of(
                        new AgentSkill.Builder()
                                .id("creative_writing")
                                .name("创意写作")
                                .description("根据给定主题创作富有想象力的短篇故事，约300字")
                                .tags(List.of("写作", "创意", "故事"))
                                .examples(List.of(
                                        "写一个关于龙与魔法师的故事",
                                        "创作一个关于太空探险的短篇故事",
                                        "写一个关于友情的故事"
                                ))
                                .build()
                ))
                .build();
    }
}
