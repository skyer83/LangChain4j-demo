package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.AstrologyAgent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 非 AI 代理 - 人类参与环节（Human-in-the-loop）
 * @author shenjh
 * @version 1.0
 * @since 2026/7/16 16:19
 */
@RestController
@RequestMapping("/astrology")
public class AstrologyController {

    @Autowired
    private ChatModel gptChatModel;

    @GetMapping("/getAstrology")
    public String getAstrology(@RequestParam(value = "sign", defaultValue = "天秤") String sign) {
        String userSign = sign.isBlank() ? "天秤" : sign.trim();

        AstrologyAgent astrologyAgent = AgenticServices
                .agentBuilder(AstrologyAgent.class)
                .chatModel(gptChatModel)
                .build();

        HumanInTheLoop humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("负责向用户询问其星座的 AI 助手。")
                .outputKey("sign")
                .responseProvider(agenticScope -> userSign)
                .build();

        SupervisorAgent supervisorAgent = AgenticServices
                .supervisorBuilder()
                .chatModel(gptChatModel)
                .subAgents(astrologyAgent, humanInTheLoop)
                .build();

        return supervisorAgent.invoke("我叫张三，请帮我看看我的星座运势。");
    }

    @GetMapping("/getAstrologyHumanInTheLoop")
    public String getAstrologyHumanInTheLoop() {

        AstrologyAgent astrologyAgent = AgenticServices
                .agentBuilder(AstrologyAgent.class)
                .chatModel(gptChatModel)
                .build();

        // 构建与人类有互动能力的 HumanInTheLoop
        HumanInTheLoop humanInTheLoop = null;

        SupervisorAgent supervisorAgent = AgenticServices
                .supervisorBuilder()
                .chatModel(gptChatModel)
                .subAgents(astrologyAgent, humanInTheLoop)
                .build();

        return supervisorAgent.invoke("我叫张三，请帮我看看我的星座运势。");
    }
}
