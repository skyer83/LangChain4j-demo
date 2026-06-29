package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.config.AgenticConfiguration;
import com.lulala.langchain4j.agentic.service.AudienceEditor;
import com.lulala.langchain4j.agentic.service.CreativeWriter;
import com.lulala.langchain4j.agentic.service.StyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 创建小说 - 循环工作流（Loop workflow）
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@RestController
@RequestMapping("/novelCreator")
public class NovelCreatorSequentialController {

    @Autowired
    private ChatModel openAiChatModel;
    @Autowired
    private ChatModel gptChatModel;
    @Autowired
    @Qualifier(AgenticConfiguration.BEAN_NAME_NovelCreatorZh)
    private UntypedAgent novelCreatorZh;

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
    public String createNovelZh(@RequestParam("topic") String topic, @RequestParam("style") String style, @RequestParam("audience") String audience) {
//        Map<String, Object> input = Map.of(
//                "topic", "龙与法师",
//                "style", "奇幻",
//                "audience", "年轻人"
//        );
        Map<String, Object> input = Map.of(
                "topic", topic,
                "style", style,
                "audience", audience
        );
        return (String) novelCreatorZh.invoke(input);
    }
}
