package com.lulala.langchain4j.openai.listener;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.spring.event.AiServiceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 监听 AI Service 注册事件，在 AI Service 注册到 Spring 容器时触发<br/>
 * 参见：https://langchain4j.cn/tutorials/spring-boot-integration.html#监听-ai-service-注册事件
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 13:33
 */
@Component
public class AiServiceRegisteredEventListener implements ApplicationListener<AiServiceRegisteredEvent> {

    @Override
    public void onApplicationEvent(AiServiceRegisteredEvent event) {
        // 获取已注册的 AI Service 及其工具信息
        // 加了 dev.langchain4j.agent.tool.Tool 注解（@Tool）的方法才会触发
        Class<?> aiServiceClass = event.aiServiceClass();
        List<ToolSpecification> toolSpecificationList = event.toolSpecifications();
        for (int i = 0; i < toolSpecificationList.size(); i++) {
            System.out.printf("[%s]: [Tool-%s]: %s%n", aiServiceClass.getSimpleName(), i + 1, toolSpecificationList.get(i));
        }
    }
}
