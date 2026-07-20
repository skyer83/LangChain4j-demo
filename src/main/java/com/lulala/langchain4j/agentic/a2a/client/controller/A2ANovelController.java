package com.lulala.langchain4j.agentic.a2a.client.controller;

import cn.hutool.core.util.StrUtil;
import com.lulala.langchain4j.agentic.a2a.client.service.A2ACreativeWriter;
import com.lulala.langchain4j.agentic.a2a.client.service.StorySupervisor;
import com.lulala.langchain4j.agentic.a2a.client.service.StyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String createNovel(@RequestParam(value = "topic", defaultValue = "龙与魔法师的冒险") String topic,
                              @RequestParam(value = "style", defaultValue = "喜剧") String style) {

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
                .supervisorContext("""
                        执行顺序：
                        1. 必须先调用 generateStory，使用 topic 作为主题生成故事，并把结果保存为 story。
                        2. 必须再调用 editStory，使用上一步的 story 和用户给定的 style 改写故事。
                        3. 最终只返回 editStory 得到的故事正文。如果某一步返回空内容，重新调用对应代理，不要直接返回空字符串。
                        """)
                .subAgents(creativeWriter, styleEditor)
                .responseStrategy(SupervisorResponseStrategy.LAST)
                .build();

        // 步骤 4: 调用监督代理（端到端测试）
        System.out.println("🚀 开始执行端到端工作流...");
        System.out.println("主题: " + topic);
        System.out.println("风格: " + style);
        System.out.println("─────────────────────────────");

        String finalStory = supervisor.createStyledStory(topic, style);
        if (StrUtil.isBlank(finalStory)) {
            System.out.println("监督代理返回空内容，改为按顺序直接调用远程写作代理和本地风格编辑代理。");
            String story = creativeWriter.generateStory(topic);
            finalStory = StrUtil.isNotBlank(story) ? styleEditor.editStory(story, style) : story;
            if (StrUtil.isBlank(finalStory)) {
                finalStory = story;
            }
        }

        System.out.println("─────────────────────────────");
        System.out.println("✅ 最终生成的故事:");
        System.out.println(finalStory);
        return finalStory;
    }
}
