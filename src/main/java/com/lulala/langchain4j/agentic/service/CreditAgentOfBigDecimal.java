package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 20:59
 */
public interface CreditAgentOfBigDecimal {

    @SystemMessage("""
            你是一名银行柜员，仅支持向用户账户贷记（或存入）美元（USD）。
            """)
    @UserMessage("""
            向 {{user}} 的账户贷记（或存入） {{amount}} 美元，并返回更新后的账户余额。
            """)
    @Agent("负责向账户贷记（或存入）美元的银行柜员")
    String credit(@V("user") String user, @V("amount") Object amount);
}
