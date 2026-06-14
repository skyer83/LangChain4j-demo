package com.lulala.langchain4j.agentic.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class AgenticAiServiceConfiguration implements BeanDefinitionRegistryPostProcessor,
        BeanFactoryAware, ResourceLoaderAware {

    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isIndependent()
                                && beanDefinition.getMetadata().isInterface();
                    }
                };
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AgenticAiService.class));

        for (String basePackage : basePackages()) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerAgenticAiService(registry, candidate);
            }
        }
    }

    private Set<String> basePackages() {
        try {
            return new LinkedHashSet<>(AutoConfigurationPackages.get(beanFactory));
        } catch (IllegalStateException ignored) {
            return Set.of("com.lulala");
        }
    }

    private void registerAgenticAiService(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        try {
            String beanClassName = candidate.getBeanClassName();
            Class<?> agentServiceClass = ClassUtils.forName(beanClassName, resourceLoader.getClassLoader());
            Map<String, Object> attributes = ((AnnotatedBeanDefinition) candidate)
                    .getMetadata()
                    .getAnnotationAttributes(AgenticAiService.class.getName());
            String chatModel = (String) attributes.get("chatModel");

            RootBeanDefinition beanDefinition = new RootBeanDefinition(AgenticAiServiceFactoryBean.class);
            beanDefinition.getPropertyValues().add("agentServiceClass", agentServiceClass);
            beanDefinition.getPropertyValues().add("chatModelBeanName", chatModel);
            beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, agentServiceClass);

            String beanName = ClassUtils.getShortNameAsProperty(agentServiceClass);
            if (registry.containsBeanDefinition(beanName)) {
                beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
            }
            registry.registerBeanDefinition(beanName, beanDefinition);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load agentic AI service class", e);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
