package com.lulala.langchain4j.agentic.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 21:15
 */
public class BankToolOfBigDecimal {

    private final Map<String, BigDecimal> accounts = new HashMap<>();

    public void createAccount(String user, BigDecimal initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("账号[" + user + "]已经存在");
        }
        accounts.put(user, initialBalance);
    }

    public BigDecimal getBalance(String user) {
        BigDecimal balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("用户[" + user + "]不存在");
        }
        return balance;
    }

    @Tool("向该用户账户贷记（或存入）指定金额，并返回更新后的账户余额。")
    public BigDecimal credit(@P("user") String user, @P("amount") BigDecimal amount) {
        BigDecimal balance = getBalance(user);
        BigDecimal newBalance = balance.add(amount);
        accounts.put(user, newBalance);
        return newBalance;
    }

    @Tool("从该用户账户扣款（或支取）指定金额，并返回更新后的账户余额。")
    public BigDecimal withdraw(@P("user") String user, @P("amount") BigDecimal amount) {
        BigDecimal balance = getBalance(user);
        BigDecimal newBalance = balance.subtract(amount);
        accounts.put(user, newBalance);
        return newBalance;
    }

}
