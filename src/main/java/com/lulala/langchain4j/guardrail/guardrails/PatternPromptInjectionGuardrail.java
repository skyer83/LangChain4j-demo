package com.lulala.langchain4j.guardrail.guardrails;

import com.lulala.langchain4j.guardrail.guardrails.official.PatternBasedPromptInjectionGuardrail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/9/4 10:50
 */
public class PatternPromptInjectionGuardrail extends PatternBasedPromptInjectionGuardrail {

    // 定义针对中文环境的自定义正则规则（建议开启大小写不敏感和Unicode支持）
    private static final List<Pattern> CHINESE_INJECTION_PATTERNS = List.of(
            // 1. 指令覆盖 (Instruction Override)
            Pattern.compile("忽略(之前|以前|所有|上面|以下)?(的)?(指令|规则|设定|限制|约束|提示词)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("无视(之前|以前|所有|上面|以下)?(的)?(指令|规则|设定|限制|约束)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("忘记(之前|以前|所有|上面|以下)?(的)?(指令|规则|设定|限制|约束|上下文)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("不要(再)?(遵循|遵守|执行|执行之前)(的)?(指令|规则|设定)", Pattern.CASE_INSENSITIVE),

            // 2. 角色劫持 (Role Hijacking)
            Pattern.compile("你现在(是|扮演|变成|作为)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("请你(扮演|作为|假装|变成)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("从现在开始(你|你就是)", Pattern.CASE_INSENSITIVE),

            // 3. 越狱与绕过安全限制 (Jailbreaks)
            Pattern.compile("进入(开发者|上帝|无限制|自由|绝对服从)模式", Pattern.CASE_INSENSITIVE),
            Pattern.compile("绕过(安全|内容|审查|道德|伦理)(限制|检查|过滤|机制)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("解除(所有)?(安全|内容|审查|道德|伦理)(限制|检查|过滤|机制)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("不受(任何)?(安全|内容|审查|道德|伦理)(限制|约束)", Pattern.CASE_INSENSITIVE),

            // 4. 系统提示词泄露 (System Prompt Leakage)
            Pattern.compile("输出(你的|初始|系统)?(提示词|指令|规则|设定|系统消息)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("打印(你的|初始|系统)?(提示词|指令|规则|设定|系统消息)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("显示(你的|初始|系统)?(提示词|指令|规则|设定|系统消息)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你的(初始|系统)?(提示词|指令|规则|设定|系统消息)(是什么|是啥)", Pattern.CASE_INSENSITIVE)
    );

    public PatternPromptInjectionGuardrail() {
        super(CHINESE_INJECTION_PATTERNS);
    }

    public PatternPromptInjectionGuardrail(List<Pattern> additionalPatterns) {
        super(combinePatterns(additionalPatterns));
    }

    private static List<Pattern> combinePatterns(List<Pattern> additionalPatterns) {
        List<Pattern> combinedPatterns = new ArrayList<>(CHINESE_INJECTION_PATTERNS.size() + additionalPatterns.size());
        combinedPatterns.addAll(CHINESE_INJECTION_PATTERNS);
        combinedPatterns.addAll(additionalPatterns);
        return combinedPatterns;
    }
}
