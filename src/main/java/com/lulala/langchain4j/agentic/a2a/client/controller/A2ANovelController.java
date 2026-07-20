package com.lulala.langchain4j.agentic.a2a.client.controller;

import com.lulala.langchain4j.agentic.a2a.client.service.A2ACreativeWriter;
import com.lulala.langchain4j.agentic.a2a.client.service.StorySupervisor;
import com.lulala.langchain4j.agentic.a2a.client.service.StyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/17 17:28
 */
@RestController
@RequestMapping("/a2a/client")
public class A2ANovelController {

    @Value("${server.port}")
    private int port;

    @Autowired
    private ChatModel gptChatModel;

    @GetMapping("/createNovel")
    public String createNovel() {

        // 步骤 1: 创建远程 A2A 代理
        String A2A_SERVER_URL = "http://localhost:" + port;
        A2ACreativeWriter creativeWriter = AgenticServices
                // 参见 io.a2a.client.http.A2ACardResolver，会先请求“域名 + 端口 + /.well-known/agent-card.json”获知对应 A2A 代理具备的能力
                // 如：http://localhost:18081/.well-known/agent-card.json
                .a2aBuilder(A2A_SERVER_URL, A2ACreativeWriter.class)
                .outputKey("story")
                .build();

        // 步骤 2: 创建本地风格编辑代理
        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(gptChatModel)
                .outputKey("story")
                .build();

        // 步骤 3: 创建监督代理，编排远程 + 本地代理
        StorySupervisor supervisor = AgenticServices
                .supervisorBuilder(StorySupervisor.class)
                .chatModel(gptChatModel)
                .subAgents(creativeWriter, styleEditor)
                .build();

        // 步骤 4: 调用监督代理（端到端测试）
        String topic = "龙与魔法师的冒险";
        String style = "喜剧";

        System.out.println("🚀 开始执行端到端工作流...");
        System.out.println("主题: " + topic);
        System.out.println("风格: " + style);
        System.out.println("─────────────────────────────");

        String finalStory = supervisor.createStyledStory(topic, style);

        System.out.println("─────────────────────────────");
        System.out.println("✅ 最终生成的故事:");
        System.out.println(finalStory);
        return finalStory;
    }
}
