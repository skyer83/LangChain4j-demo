package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.service.LegalExpert;
import com.lulala.langchain4j.toolspecification.service.MedicalExpert;
import com.lulala.langchain4j.toolspecification.service.RouterAgent;
import com.lulala.langchain4j.toolspecification.service.TechnicalExpert;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/tools.html<br/>
 * 将 AI Service 作为其他 AI Service 的工具
 * @author shenjh
 * @version 1.0
 * @since 2026/8/20 17:33
 */
@Slf4j
@RestController
@RequestMapping("/toolspecification/ai-service-tool")
public class AiServiceToolController {

    @Autowired
    private ChatModel deepseekChatModel;

    /**
     * 分类咨询
     * @return java.lang.String
     * @author shenjh
     * @since 2026/6/15 11:58
     */
    @GetMapping("/ask")
    public String ask() {
        MedicalExpert medicalExpert = AiServices.builder(MedicalExpert.class).chatModel(deepseekChatModel).build();
        LegalExpert legalExpert = AiServices.builder(LegalExpert.class).chatModel(deepseekChatModel).build();
        TechnicalExpert technicalExpert = AiServices.builder(TechnicalExpert.class).chatModel(deepseekChatModel).build();
        RouterAgent routerAgent = AiServices.builder(RouterAgent.class)
                .chatModel(deepseekChatModel)
                .tools(medicalExpert, legalExpert, technicalExpert)
                .build();
        return routerAgent.askToExpert("我的腿摔断了（或者骨折了），我该怎么办？");
    }

}
