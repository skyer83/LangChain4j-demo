package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.enums.RequestCategory;
import com.lulala.langchain4j.agentic.service.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 咨询分类 - 条件工作流（Conditional workflow）
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@RestController
@RequestMapping("/requestCategoryConditional")
public class RequestCategoryConditionalController {

    @Autowired
    private ChatModel gptChatModel;

    /**
     * 分类咨询
     * @return java.lang.String
     * @author shenjh
     * @since 2026/6/15 11:58
     */
    @GetMapping("/ask")
    public String ask() {
        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(gptChatModel)
                .outputKey("category")
                .build();

        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(gptChatModel)
                .outputKey("response")
                .build();
        LegalExpert legalExpert = AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(gptChatModel)
                .outputKey("response")
                .build();
        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(gptChatModel)
                .outputKey("response")
                .build();
        UnknownExpert unknownExpert = AgenticServices
                .agentBuilder(UnknownExpert.class)
                .chatModel(gptChatModel)
                .outputKey("response")
                .build();

        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.UNKNOWN, unknownExpert)
                .build();

        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();

//        return expertRouterAgent.ask("我的腿摔断了（或者骨折了），我该怎么办？");
//        return expertRouterAgent.ask("浩东和小常离婚了，浩东该怎么办？");
        return expertRouterAgent.ask("外星人真的存在吗？");
    }
}
