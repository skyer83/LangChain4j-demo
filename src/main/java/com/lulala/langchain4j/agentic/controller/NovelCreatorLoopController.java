package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.CreativeWriter;
import com.lulala.langchain4j.agentic.service.StyleEditor;
import com.lulala.langchain4j.agentic.service.StyleScorer;
import com.lulala.langchain4j.agentic.service.StyledWriter;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 创建小说 - 循环工作流（Loop workflow）
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@RestController
@RequestMapping("/novelCreatorLoop")
public class NovelCreatorLoopController {

    @Autowired
    private ChatModel deepseekChatModel;

    @GetMapping("/createNovel")
    public String createNovel() {
        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(deepseekChatModel)
                .outputKey("story")
                .build();

        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(deepseekChatModel)
                .outputKey("score")
                .build();

        UntypedAgent styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition(agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
                .build();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(deepseekChatModel)
                .outputKey("story")
                .build();

        StyledWriter styledWriter = AgenticServices
                .sequenceBuilder(StyledWriter.class)
                .subAgents(creativeWriter, styleReviewLoop)
                .outputKey("story")
                .build();

        // 巨龙与巫师，喜剧
        return styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
    }
}
