package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * 本质上，在 langchain4j-agentic 中，一个代理就是任意只有一个方法且带有 @Agent 注解的 Java 类
 * @author shenjh
 * @version 1.0
 * @since 2026/7/16 14:47
 */
public class ExchangeOperator {

    private static final Map<String, BigDecimal> EXCHANGE_RATES = Map.of(
            "USD:CNY", BigDecimal.valueOf(7.20),
            "CNY:USD", BigDecimal.valueOf(0.14),
            "USD:EUR", BigDecimal.valueOf(0.92),
            "EUR:USD", BigDecimal.valueOf(1.09),
            "USD:GBP", BigDecimal.valueOf(0.79),
            "GBP:USD", BigDecimal.valueOf(1.27),
            "EUR:CNY", BigDecimal.valueOf(7.83),
            "CNY:EUR", BigDecimal.valueOf(0.13),
            "GBP:CNY", BigDecimal.valueOf(9.14),
            "CNY:GBP", BigDecimal.valueOf(0.11)
    );

    private static final Map<String, String> CURRENCY_ALIASES = Map.ofEntries(
            Map.entry("USD", "USD"),
            Map.entry("美元", "USD"),
            Map.entry("美金", "USD"),
            Map.entry("EUR", "EUR"),
            Map.entry("欧元", "EUR"),
            Map.entry("CNY", "CNY"),
            Map.entry("RMB", "CNY"),
            Map.entry("人民币", "CNY"),
            Map.entry("人民币元", "CNY"),
            Map.entry("GBP", "GBP"),
            Map.entry("英镑", "GBP")
    );

    @Agent(outputKey = "exchange", value = "负责将指定金额从原币种兑换为目标币种的货币兑换员。")
    // 非 AI 代理由 agentic 直接反射调用，这里放宽 amount 入参，避免 Double/Integer/BigDecimal 类型不一致导致调用失败。
    public BigDecimal exchange(@V("originalCurrency") String originalCurrency, @V("amount") Object amount, @V("targetCurrency") String targetCurrency) {
        BigDecimal amountValue = toBigDecimal(amount);
        if (amountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("兑换金额不能为负数");
        }

        String original = normalizeCurrency(originalCurrency);
        String target = normalizeCurrency(targetCurrency);
        if (original.equals(target)) {
            return amountValue;
        }

        BigDecimal exchangeRate = EXCHANGE_RATES.get(original + ":" + target);
        if (exchangeRate == null) {
            throw new IllegalArgumentException("暂不支持从 " + original + " 到 " + target + " 的货币兑换");
        }
        return amountValue.multiply(exchangeRate);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("币种不能为空");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        return CURRENCY_ALIASES.getOrDefault(normalized, normalized);
    }

    private BigDecimal toBigDecimal(Object amount) {
        if (amount == null) {
            throw new IllegalArgumentException("兑换金额不能为空");
        }
        if (amount instanceof BigDecimal value) {
            return value;
        }
        if (amount instanceof Number value) {
            return BigDecimal.valueOf(value.doubleValue());
        }
        if (amount instanceof String value && !value.isBlank()) {
            return new BigDecimal(value.trim());
        }
        throw new IllegalArgumentException("不支持的兑换金额类型: " + amount.getClass().getName());
    }
}
