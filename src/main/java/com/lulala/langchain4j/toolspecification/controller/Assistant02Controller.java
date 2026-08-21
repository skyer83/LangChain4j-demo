package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.service.Assistant;
import com.lulala.langchain4j.toolspecification.tools.BookingTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/addBooking")
    public String addBooking(@RequestParam("message") String message) {
        Assistant assistant = AiServices.builder(Assistant.class).chatModel(deepseekChatModel).tools(bookingTools).build();
        Result<String> chatResult = assistant.chat(message);
        List<ToolExecution> toolExecutions = chatResult.toolExecutions();
        log.info("Tool Executions: {}", toolExecutions);
        return chatResult.content();
    }

    @GetMapping("/getBookingDetails")
    public String getBookingDetails() {
        return "getBookingDetails";
    }

    @GetMapping("/cancelBooking")
    public String cancelBooking() {
        return "cancelBooking";
    }
}
