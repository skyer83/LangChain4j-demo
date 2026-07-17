package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.AstrologyAgent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/getAstrologyHumanInTheLoop", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String getAstrologyHumanInTheLoop(@RequestParam(value = "sign", required = false) String sign) {
        String userSign = sign == null || sign.isBlank() ? null : sign.trim();

        AstrologyAgent astrologyAgent = AgenticServices
                .agentBuilder(AstrologyAgent.class)
                .chatModel(gptChatModel)
                .build();

        // 构建与人类有互动能力的 HumanInTheLoop
        HumanInTheLoop humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("当用户请求星座运势但缺少星座时，向用户询问星座，并返回用户提供的星座。不要编造星座。")
                .outputKey("sign")
                .responseProvider(agenticScope -> {
                    if (userSign == null) {
                        throw new HumanInputRequiredException("智能体判断还缺少星座信息，请补充你的星座。");
                    }
                    return userSign;
                })
                .build();

        SupervisorAgent supervisorAgent = AgenticServices
                .supervisorBuilder()
                .chatModel(gptChatModel)
                .subAgents(astrologyAgent, humanInTheLoop)
                .build();

        try {
            String result = supervisorAgent.invoke("我叫张三，请帮我看看我的星座运势。");
            return renderAstrologyResult(userSign, result);
        } catch (AgentInvocationException e) {
            HumanInputRequiredException inputRequired = findCause(e, HumanInputRequiredException.class);
            if (inputRequired != null) {
                return renderAstrologyForm(inputRequired.getMessage());
            }
            throw e;
        }
    }

    private String renderAstrologyForm(String message) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <title>Human-in-the-loop 星座运势</title>
                </head>
                <body>
                    <h3>请补充星座信息</h3>
                    <p>%s</p>
                    <form action="/astrology/getAstrologyHumanInTheLoop" method="get">
                        <label for="sign">星座：</label>
                        <input id="sign" name="sign" placeholder="例如：白羊、天秤、双鱼" required>
                        <button type="submit">提交</button>
                    </form>
                </body>
                </html>
                """.formatted(escapeHtml(message));
    }

    private String renderAstrologyResult(String sign, String result) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <title>Human-in-the-loop 星座运势</title>
                </head>
                <body>
                    <h3>星座运势</h3>
                    <p>用户补充的星座：%s</p>
                    <pre>%s</pre>
                    <a href="/astrology/getAstrologyHumanInTheLoop">重新输入</a>
                </body>
                </html>
                """.formatted(escapeHtml(sign), escapeHtml(result));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static class HumanInputRequiredException extends RuntimeException {

        private HumanInputRequiredException(String message) {
            super(message);
        }
    }
}
