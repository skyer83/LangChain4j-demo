package com.lulala.langchain4j.openai.controller;

import com.lulala.langchain4j.openai.service.ChatBot;
import com.lulala.langchain4j.openai.service.GreetingExpert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多个 AI 服务配合使用（https://langchain4j.cn/tutorials/ai-services.html#链接多个-ai-服务）
 * <pre>
 *  我想为我的公司构建一个聊天机器人。
 *  如果用户向聊天机器人打招呼，
 *  我希望它用预定义的问候语进行响应，而不是依赖 LLM 来生成问候语。
 *  如果用户提问，我希望 LLM 使用公司的内部知识库（即 RAG）生成响应。
 * </pre>
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 17:10
 */
@RestController
@RequestMapping("/openai/miles-of-smiles")
public class MilesOfSmilesController {

    @Autowired
    private ChatBot chatBot;
    @Autowired
    private GreetingExpert greetingExpert;

    @RequestMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        if (greetingExpert.isGreeting(message)) {
            return "来自Miles of Smiles的问候！有什么能为您效劳，让您今日心情更愉悦？";
        }
        return chatBot.chat(message);
    }

}
