package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.service.Assistant;
import com.lulala.langchain4j.toolspecification.tools.BookingTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 参见：https://langchain4j.cn/tutorials/tools.html<br/>
 * 访问已执行的工具
 * @author shenjh
 * @version 1.0
 * @since 2026/8/21 10:13
 */
@Slf4j
@RestController
@RequestMapping("/toolspecification/assistant")
public class Assistant02Controller {

    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private StreamingChatModel deepseekStreamingChatModel;
    @Autowired
    private BookingTools bookingTools;

    @GetMapping("/getAllBooking")
    public String getAllBooking(@RequestParam("message") String message) {
        // 获取所有预约详情
        return chat(message);
    }

    @GetMapping("/addBooking")
    public String addBooking(@RequestParam("message") String message) {
        // 我叫张三，帮我预订一下
        return chat(message);
    }

    @GetMapping("/getBookingDetails")
    public String getBookingDetails(@RequestParam("message") String message) {
        // 获取 xx 号的预约详情
        return chat(message);
    }

    @GetMapping("/cancelBooking")
    public String cancelBooking(@RequestParam("message") String message) {
        // 取消 xx 号的预约
        return chat(message);
    }

    private String chat(String message) {
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(deepseekChatModel).tools(bookingTools).build();
        Result<String> chatResult = assistant.chat(message);
        List<ToolExecution> toolExecutions = chatResult.toolExecutions();
        log.info("Tool Executions: {}", toolExecutions);
        return chatResult.content();
    }

    @GetMapping("/getAllBookingStreaming")
    public SseEmitter getAllBookingStreaming(@RequestParam("message") String message) {
        // 获取所有预约详情
        SseEmitter emitter = new SseEmitter(0L);
        streamingChat(message, emitter);
        return emitter;
    }

    private void streamingChat(String message, SseEmitter emitter) {
        log.info("[streamingChat] 收到请求, message: {}", message);
        StringBuilder fullContent = new StringBuilder();

        Assistant assistant = AiServices.builder(Assistant.class).streamingChatModel(deepseekStreamingChatModel).tools(bookingTools).build();
        TokenStream tokenStream = assistant.chatStream(message);
        tokenStream.onToolExecuted(toolExecution -> {
            log.info("[streamingChat] Tool Executed: {}", toolExecution);
        });
        tokenStream.onPartialResponse(part -> {
            fullContent.append(part);
            try {
                emitter.send(SseEmitter.event().data(part));
            } catch (Exception e) {
                log.error("[streamingChat] SSE发送失败", e);
            }
        });
        tokenStream.onCompleteResponse(response -> {
            log.info("[streamingChat] 流式响应完成, 完整内容: {}", fullContent);
            log.info("[streamingChat] token用量 - input: {}, output: {}",
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount());
            emitter.complete();
        });
        tokenStream.onError(error -> {
            log.error("[streamingChat] 流式响应异常, 已接收部分内容: {}", fullContent, error);
            emitter.completeWithError(error);
        });
        tokenStream.start();
    }
}
