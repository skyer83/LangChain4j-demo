package com.lulala.langchain4j.openai.controller;

import com.lulala.langchain4j.openai.service.IAssistant;
import com.lulala.langchain4j.openai.service.IOllamaAiAssistant;
import com.lulala.langchain4j.openai.service.IOpenAiAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 11:30
 */
@RestController
@RequestMapping("/openai/assistant")
public class AssistantController {

    @Autowired
    private IAssistant aiAssistant;
    @Autowired
    private IOpenAiAssistant openAiAssistant;
    @Autowired
    private IOllamaAiAssistant ollamaAiAssistant;

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return aiAssistant.chat(message);
    }

    @GetMapping("/chat4OpenAi")
    public String chat4OpenAi(@RequestParam(value = "message") String message) {
        return openAiAssistant.chat(message);
    }

    @GetMapping("/chat4OllamaAi")
    public String chat4OllamaAi(@RequestParam(value = "message") String message) {
        return ollamaAiAssistant.chat(message);
    }
}
