package com.lulala.langchain4j.agentic.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 21:31
 */
public class ExchangeTool {

    private static final Map<String, Double> EXCHANGE_RATES = Map.of(
            "USD:CNY", 7.20,
            "CNY:USD", 0.14,
            "USD:EUR", 0.92,
            "EUR:USD", 1.09,
            "USD:GBP", 0.79,
            "GBP:USD", 1.27,
            "EUR:CNY", 7.83,
            "CNY:EUR", 0.13,
            "GBP:CNY", 9.14,
            "CNY:GBP", 0.11
    );

    @Tool("将指定金额从原币种转换为目标币种。")
    Double exchange(@P("originalCurrency") String originalCurrency, @P("amount") Double amount, @P("targetCurrency") String targetCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("兑换金额不能为空");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("兑换金额不能为负数");
        }

        String original = normalizeCurrency(originalCurrency);
        String target = normalizeCurrency(targetCurrency);
        if (original.equals(target)) {
            return amount;
        }

        Double exchangeRate = EXCHANGE_RATES.get(original + ":" + target);
        if (exchangeRate == null) {
            throw new IllegalArgumentException("暂不支持从 " + original + " 到 " + target + " 的货币兑换");
        }
        return amount * exchangeRate;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("币种不能为空");
        }
        return currency.trim().toUpperCase();
    }

}
