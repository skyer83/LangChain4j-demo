package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 21:04
 */
public interface ExchangeAgent {

    /*
        @UserMessage 可以修改为 @SystemMessage 吗？

        可以改，但不建议把整个 @UserMessage 直接替换成 @SystemMessage。

        原因是两者语义不同：

        @SystemMessage：告诉模型“你是谁、必须遵守什么规则”，属于角色和行为约束。
        @UserMessage：表示本次调用的具体用户任务，也就是这次要兑换多少、从什么币种到什么币种。
        你现在的 ExchangeAgent.java 里这段内容其实混合了两类信息：

        你是一名负责不同货币兑换的操作员。
        请使用工具将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}，
        并且仅返回工具提供的最终金额，保持原样，不要附加任何其他内容。

        更合理的写法是拆开：

        @SystemMessage("""
                你是一名负责不同货币兑换的操作员。
                你必须使用工具完成兑换。
                仅返回工具提供的最终金额，保持原样，不要附加任何其他内容。
                """)
        @UserMessage("""
                请将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}。
                """)
        @Agent("负责将指定金额从原币种兑换为目标币种的货币兑换员")
        Double exchange(@V("originalCurrency") String originalCurrency,
                        @V("amount") Double amount,
                        @V("targetCurrency") String targetCurrency);

        直接只用 @SystemMessage 的问题是：这次调用的具体兑换请求也会变成 system 角色内容，语义上不对；模型会缺少明确的 user message。某些情况下可能还能跑，但不符合 LangChain4j AI Service 的常规用法，也不利于后续维护、调试和多轮/agentic workflow 编排。

        结论：角色、规则、输出约束可以放到 @SystemMessage；具体兑换任务保留在 @UserMessage。
     */
    @UserMessage("""
            你是一名负责不同货币兑换的操作员。
            请使用工具将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}，
            并且仅返回工具提供的最终金额，保持原样，不要附加任何其他内容。
            """)
    @Agent("负责将指定金额从原币种兑换为目标币种的货币兑换员")
    Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);
}
