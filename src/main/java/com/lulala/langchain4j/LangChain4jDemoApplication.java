package com.lulala.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * @author shenjh
 * @since 2026/6/5 17:49
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = {"com.lulala"})
public class LangChain4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jDemoApplication.class, args);
    }

}
