package com.lulala.langchain4j.guardrail.guardrails;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.model.chat.ChatModel;

/**
 * 幻觉检测护栏
 * @author shenjh
 * @version 1.0
 * @since 2026/9/4 10:20
 */
public class HallucinationOutputGuardrail implements OutputGuardrail {

    private final ChatModel judgeModel;
    private final String context;

    public HallucinationOutputGuardrail(ChatModel judgeModel, String context) {
        this.judgeModel = judgeModel;
        this.context = context;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String prompt = "你是一个事实核查员。请根据提供的【参考上下文】，判断【AI的回答】是否完全基于上下文内容。\n" +
                "如果回答中包含了上下文中不存在的信息，或者与上下文矛盾，请判定为幻觉。\n" +
                "请仅回答 'YES'（没有幻觉）或 'NO'（存在幻觉）。\n\n" +
                "【参考上下文】:\n" + context + "\n\n" +
                "【AI的回答】:\n" + responseFromLLM.text();

        String judgeResponse = judgeModel.chat(
                SystemMessage.from("你是一个严格的事实核查AI，只回答 YES 或 NO。"),
                UserMessage.from(prompt)
        ).aiMessage().text();

        if (judgeResponse != null && judgeResponse.trim().toUpperCase().contains("NO")) {
            return fatal("检测到回答包含未在参考上下文中提供的信息（幻觉），已拦截。");
        }
        return OutputGuardrailResult.success();
    }
}
