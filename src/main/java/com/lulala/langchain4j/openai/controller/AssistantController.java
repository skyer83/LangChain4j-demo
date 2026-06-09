package com.lulala.langchain4j.openai.controller;

import com.lulala.langchain4j.openai.service.IAssistant;
import com.lulala.langchain4j.openai.service.IAssistantOfStreaming;
import com.lulala.langchain4j.openai.service.IOllamaAiAssistant;
import com.lulala.langchain4j.openai.service.IOpenAiAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
    private IAssistantOfStreaming aiAssistantOfStreaming;
    @Autowired
    private IOpenAiAssistant openAiAssistant;
    @Autowired
    private IOllamaAiAssistant ollamaAiAssistant;

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return aiAssistant.chat(message);
    }
    
    /**
     * 没有流式输出效果
     * <pre>
     *     Spring MVC 返回 Flux<String> 时，默认使用 application/json 序列化。浏览器会等待服务器将整个 Flux 的所有元素收集完毕、打包成 JSON 数组后才一次性展示，
     *     这就是为什么"看不到流式效果"
     * </pre>
     * @param message
     * @return reactor.core.publisher.Flux<java.lang.String> 
     * @author shenjh
     * @since 2026/6/9 9:28
     */
    @GetMapping(value = "/chatFlux")
    public Flux<String> chatFlux(@RequestParam(value = "message") String message) {
        return aiAssistantOfStreaming.chatFlux(message);
    }
    
    /**
     * 有流式输出效果
     * <pre>
     *     加上 produces = MediaType.TEXT_EVENT_STREAM_VALUE 后，Spring MVC 会以 SSE（Server-Sent Events）协议逐个推送每个 token，
     *     浏览器就能实时看到逐字输出的流式效果了。
     * </pre>
     * @param message
     * @return reactor.core.publisher.Flux<java.lang.String> 
     * @author shenjh
     * @since 2026/6/9 9:29
     */
    @GetMapping(value = "/chatFluxStreaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatFluxStreaming(@RequestParam(value = "message") String message) {
        return aiAssistantOfStreaming.chatFlux(message);
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
