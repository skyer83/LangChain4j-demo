package com.lulala.langchain4j.agentic.config;

import com.lulala.langchain4j.agentic.service.AudienceEditorZh;
import com.lulala.langchain4j.agentic.service.CreativeWriterZh;
import com.lulala.langchain4j.agentic.service.StyleEditorZh;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgenticConfiguration {

    public static final String BEAN_NAME_NovelCreatorZh = "novelCreatorZh";
    @Bean(BEAN_NAME_NovelCreatorZh)
    UntypedAgent novelCreatorZh(ApplicationContext applicationContext) {
        // CreativeWriterZh、AudienceEditorZh、StyleEditorZh 在这边作为 subAgents 后，不能再被别的 workflow Bean 复用，否则会有并发问题
        CreativeWriterZh creativeWriterZh = applicationContext.getBean(CreativeWriterZh.class);
        AudienceEditorZh audienceEditorZh = applicationContext.getBean(AudienceEditorZh.class);
        StyleEditorZh styleEditorZh = applicationContext.getBean(StyleEditorZh.class);

        return AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriterZh, audienceEditorZh, styleEditorZh)
                .outputKey("story")
                .build();
    }
}
