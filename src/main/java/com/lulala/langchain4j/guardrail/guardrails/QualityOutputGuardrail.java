package com.lulala.langchain4j.guardrail.guardrails;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * 输出质量护栏：检查 AI 的回复是否过于简短。
 * 如果回复字数低于阈值，将触发 reprompt，要求大模型重新生成更详细的回答。
 * @author shenjh
 * @version 1.0
 * @since 2026/9/4 9:45
 */
public class QualityOutputGuardrail  implements OutputGuardrail {

    // 设定回复的最小字数阈值（可根据实际业务需求调整）
    private static final int MIN_LENGTH = 10;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text();

        // 如果回复为空，或者长度低于设定的最小阈值
        if (text == null || text.length() < MIN_LENGTH) {
            // 触发 reprompt 机制：
            // 参数1: 反馈原因（记录在日志或上下文中）
            // 参数2: 追加给大模型的提示词（要求大模型重新生成）
            return reprompt(
                    "AI 回复过于简短，未达到质量要求",
                    "你上一次的回复过于简短且缺乏实质性内容。请重新生成回答，确保提供详细、准确、有深度的信息，字数至少满足要求。"
            );
        }

        // 回复质量达标，放行
        return OutputGuardrailResult.success();
    }
}
