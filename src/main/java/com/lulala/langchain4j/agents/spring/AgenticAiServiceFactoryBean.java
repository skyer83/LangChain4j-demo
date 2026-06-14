package com.lulala.langchain4j.agents.spring;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

public class AgenticAiServiceFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {

    private Class<T> agentServiceClass;
    private String chatModelBeanName;
    private ApplicationContext applicationContext;
    private T agent;

    public void setAgentServiceClass(Class<T> agentServiceClass) {
        this.agentServiceClass = agentServiceClass;
    }

    public void setChatModelBeanName(String chatModelBeanName) {
        this.chatModelBeanName = chatModelBeanName;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public T getObject() {
        if (agent == null) {
            agent = AgenticServices
                    .agentBuilder(agentServiceClass)
                    .chatModel(chatModel())
                    .build();
        }
        return agent;
    }

    private ChatModel chatModel() {
        if (StringUtils.hasText(chatModelBeanName)) {
            return applicationContext.getBean(chatModelBeanName, ChatModel.class);
        }
        return applicationContext.getBean(ChatModel.class);
    }

    @Override
    public Class<?> getObjectType() {
        return agentServiceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
