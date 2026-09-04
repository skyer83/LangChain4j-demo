package com.lulala.langchain4j.guardrail.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 格式校验护栏
 * @author shenjh
 * @version 1.0
 * @since 2026/9/4 10:17
 */
@Slf4j
public class JsonFormatOutputGuardrail implements OutputGuardrail {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text();
        try {
            MAPPER.readTree(text);
            return OutputGuardrailResult.success();
        } catch (Exception e) {
            log.warn("JSON 格式校验失败: {}", text, e);
            return reprompt(
                    "JSON 格式校验失败: " + e.getMessage(),
                    "你上一次返回的不是合法的纯 JSON 格式。请重新生成，不要包含任何 Markdown 标记（如 ```json），不要包含任何解释性文字，仅输出合法的 JSON 字符串。"
            );
        }
    }
}
