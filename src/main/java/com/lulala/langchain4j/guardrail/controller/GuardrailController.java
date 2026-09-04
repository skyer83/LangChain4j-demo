package com.lulala.langchain4j.guardrail.controller;

import com.lulala.langchain4j.guardrail.guardrails.*;
import com.lulala.langchain4j.guardrail.service.GuardrailAssistant;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/9/4 14:10
 */
@Slf4j
@RestController
@RequestMapping("/guardrail")
public class GuardrailController {

    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private ModerationModel deepseekModerationModel;

    @RequestMapping("/chat")
    public String chat(@RequestParam String message) {
        // 1. 准备中文正则规则
        List<Pattern> chinesePatterns = List.of(
                Pattern.compile("忽略(之前|所有)?(的)?(指令|规则)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("你现在(是|扮演)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("进入(开发者|无限制)模式", Pattern.CASE_INSENSITIVE)
        );

        GuardrailAssistant assistant = AiServices.builder(GuardrailAssistant.class)
                .chatModel(deepseekChatModel)
                .inputGuardrails(
                        /*
                            对用户发送给大语言模型（LLM）的消息进行内容审核（Moderation）:
                            内容安全检测：
                                仇恨言论
                                暴力内容
                                自残倾向
                                色情内容
                                其他由审核模型定义的不当类别

                            如果审核模型将某条消息标记为"已标记"（flagged），则验证失败，返回一个致命结果（fatal result）
                            并抛出 ModerationException，从而阻止该消息被进一步处理，防止不合规内容进入 LLM 或应用逻辑。

                            如果消息通过了审核（未被标记），则返回成功结果，消息可以继续正常处理。

                            目前调用 GPT 的代理 https://api.dwai.cloud/v1/moderations 也报 404，因此先不做内容审核的示例
                         */
                        //new MessageModeratorInputGuardrail(deepseekModerationModel),
                        new PatternPromptInjectionGuardrail(chinesePatterns),
                        new LlmPromptInjectionGuardrail(deepseekChatModel)
                )
                .outputGuardrails(
                        new QualityOutputGuardrail(),
                        new JsonFormatOutputGuardrail(),
                        new HallucinationOutputGuardrail(deepseekChatModel, message)
                )
                .build();
        try {
            return assistant.chat(message);
        } catch (InputGuardrailException e) {
            log.warn("输入被拦截", e);
            return "输入被拦截: " + e.getMessage();
        } catch (OutputGuardrailException e) {
            // 捕获输出护栏拦截异常
            // 如果业务需要重试，可以在这里写重试逻辑；否则直接返回友好提示
            log.warn("输出被拦截", e);
            return "输出被拦截: " + e.getMessage();
        }
    }
}
