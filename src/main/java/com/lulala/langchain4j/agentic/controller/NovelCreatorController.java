package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.*;
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
    @Autowired
    private CreativeWriterZh creativeWriterZh;
    @Autowired
    private AudienceEditorZh audienceEditorZh;
    @Autowired
    private StyleEditorZh styleEditorZh;

    @GetMapping("/createNovel")
    public String createNovel() {
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(openAiChatModel)
                .outputKey("story")
                .build();

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(openAiChatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(gptChatModel)
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
        return (String) novelCreator.invoke(input);
    }

    @GetMapping("/createNovelZh")
    public String createNovelZh() {
        UntypedAgent novelCreator = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriterZh, audienceEditorZh, styleEditorZh)
                .outputKey("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "龙与法师",
                "style", "奇幻",
                "audience", "年轻人"
        );
        return (String) novelCreator.invoke(input);
    }
}
