package com.lulala.langchain4j.toolspecification.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/27 13:52
 */
@Slf4j
public class MathTools {

    @Tool("将给定的2个数字进行相加")
    public double sum(double a, double b) {
        log.info("调用 MathTool.sum，a: {}, b: {}", a, b);
        return a + b;
    }

    @Tool("计算给定数字的平方根")
    public double squareRoot(double a) {
        log.info("调用 MathTool.squareRoot，a: {}", a);
        return Math.sqrt(a);
    }
}
