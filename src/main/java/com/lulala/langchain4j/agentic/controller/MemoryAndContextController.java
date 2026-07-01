package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.enums.RequestCategory;
import com.lulala.langchain4j.agentic.service.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 内存与上下文工程
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@Slf4j
@RestController
@RequestMapping("/memoryAndContext")
public class MemoryAndContextController {

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

        ContextSummarizer contextSummarizer = AgenticServices
                .agentBuilder(ContextSummarizer.class)
                .chatModel(gptChatModel)
                .build();

        MedicalExpertWithMemory medicalExpert = AgenticServices
                .agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();
        LegalExpertWithMemory legalExpert = AgenticServices
                .agentBuilder(LegalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .context(agenticScope -> contextSummarizer.summarize(agenticScope.contextAsConversation()))
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
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.UNKNOWN, unknownExpert)
                .build();

        ExpertRouterAgentWithMemory expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgentWithMemory.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();
        String memoryId = "1";
        String medicalRresponse = expertRouterAgent.ask(memoryId, "我的腿摔断了（或者骨折了），我该怎么办？");
        log.info("医学专家回复: {}", medicalRresponse);
        String legalResponse = expertRouterAgent.ask(memoryId, "这是邻居造成的，我该告他吗？");
        log.info("法律专家回复: {}", legalResponse);
        return legalResponse;
    }

    /**
     * 分类咨询
     * @return java.lang.String
     * @author shenjh
     * @since 2026/6/15 11:58
     */
    @GetMapping("/ask02")
    public String ask02() {
        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(gptChatModel)
                .outputKey("category")
                .build();

        MedicalExpertWithMemory medicalExpert = AgenticServices
                .agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();
        TechnicalExpertWithMemory technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();
        LegalExpertWithMemory legalExpert = AgenticServices
                .agentBuilder(LegalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .summarizedContext("medical", "technical")
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

        ExpertRouterAgentWithMemory expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgentWithMemory.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();

        String memoryId = "1";
        String medicalRresponse = expertRouterAgent.ask(memoryId, "我的失眠了，我该怎么办？");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        log.info("医学专家回复: {}", medicalRresponse);
        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        String legalResponse = expertRouterAgent.ask(memoryId, "这是邻居晚上一直吵闹造成的，我该告他吗？");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        log.info("法律专家回复: {}", legalResponse);
        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        return legalResponse;
    }
}
