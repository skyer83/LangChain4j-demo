package com.lulala.langchain4j.agents.controller;

import com.lulala.langchain4j.agents.service.AudienceEditor;
import com.lulala.langchain4j.agents.service.CreativeWriter;
import com.lulala.langchain4j.agents.service.StyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 创建小说
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@RestController
@RequestMapping("/novelCreator")
public class NovelCreatorController {

    @Autowired
    private ChatModel openAiChatModel;
    @Autowired
    private ChatModel gptChatModel;

    @GetMapping("/createNovel")
    public String createNovel() {
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(gptChatModel)
                .outputKey("story")
                .build();

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(openAiChatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(openAiChatModel)
                .outputKey("story")
                .build();

        UntypedAgent novelCreator = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "dragons and wizards",
                "style", "fantasy",
                "audience", "young adults"
        );

        String story = (String) novelCreator.invoke(input);
        return story;
    }
}
