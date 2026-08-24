package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.service.Assistant;
import com.lulala.langchain4j.toolspecification.tools.BookingTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/tools.html<br/>
 * 工具幻觉处理策略
 * @author shenjh
 * @since 2026/8/24 14:48
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/toolspecification/hallucinated")
public class AssistantHallucinatedController {

    private static final String HALLUCINATED_TOOL_TEST_01_PREFIX = "__hallucinated_tool_test_01__:";
    /** 模拟修正工具调用 */
    private static final String HALLUCINATED_TOOL_TEST_02_PREFIX = "__hallucinated_tool_test_02__:";

    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private BookingTools bookingTools;

    @GetMapping("/getAssistantHallucinated")
    public String getAssistantHallucinated(@RequestParam("message") String message) {
        if (message.startsWith(HALLUCINATED_TOOL_TEST_01_PREFIX)) {
            String toolName = message.substring(HALLUCINATED_TOOL_TEST_01_PREFIX.length()).trim();
            return runHallucinatedToolTest01(toolName);
        } else if (message.startsWith(HALLUCINATED_TOOL_TEST_02_PREFIX)) {
            String newMessage = message.substring(HALLUCINATED_TOOL_TEST_02_PREFIX.length()).trim();
            return runHallucinatedToolTest02(newMessage);
        }

        Assistant assistant = buildAssistant(deepseekChatModel);
        Result<String> chatResult = assistant.chat(message);
        log.info("Tool Executions: {}", chatResult.toolExecutions());
        return chatResult.content();
    }

    private Assistant buildAssistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(bookingTools)
                .hallucinatedToolNameStrategy(this::handleHallucinatedTool)
                .build();
    }

    /**
     * 使用一个确定返回未知工具调用的测试模型，验证 LangChain4j 会真正执行
     * hallucinatedToolNameStrategy，而不是依赖真实模型是否愿意生成未知工具名。
     */
    private String runHallucinatedToolTest02(String message) {
        ChatModel testChatModel = new ChatModel() {
            private boolean firstResponse = true;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                if (firstResponse) {
                    firstResponse = false;
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .id("hallucinated-test-call")
                            .name("getWeather")
                            .arguments("{}")
                            .build();
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from(toolExecutionRequest))
                            .build();
                }
                // 工具 'getWeather' 不存在或当前不可用。只能使用已提供的预约工具，请根据用户需求重新选择工具。
                Assistant assistant = buildAssistant(deepseekChatModel);
                Result<String> chatResult = assistant.chat(message);
                // runHallucinatedToolTest02 deepseekChatModel Tool Executions: [ToolExecution{request=ToolExecutionRequest { id = "call_00_hoJQV9Qu12gBaaaCrIRM5279", name = "getBookingDetails", arguments = "{"bookingNumber": "3"}" }, result=ToolExecutionResult {isError = false, result = Booking(id=2091788171599663106, bookingNumber=3, customerName=Mike, customerSurname=Johnson), resultContents = [TextContent { text = "{"id":2091788171599663106,"bookingNumber":"3","customerName":"Mike","customerSurname":"Johnson"}" }], attributes = {}}, startTime=null, finishTime=null}]
                log.info("runHallucinatedToolTest02 deepseekChatModel Tool Executions: {}", chatResult.toolExecutions());
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(chatResult.content()))
                        .build();
            }
        };

        Assistant assistant = buildAssistant(testChatModel);
        Result<String> chatResult = assistant.chat(message);
        // runHallucinatedToolTest02 testChatModel Tool Executions: [ToolExecution{request=ToolExecutionRequest { id = "hallucinated-test-call", name = "getWeather", arguments = "{}" }, result=ToolExecutionResult {isError = false, result = null, resultContents = [TextContent { text = "工具 'getWeather' 不存在或当前不可用。只能使用已提供的预约工具，请根据用户需求重新选择工具。" }], attributes = {}}, startTime=null, finishTime=null}]
        log.info("runHallucinatedToolTest02 testChatModel Tool Executions: {}", chatResult.toolExecutions());
        return chatResult.content();
    }

    /**
     * 使用一个确定返回未知工具调用的测试模型，验证 LangChain4j 会真正执行
     * hallucinatedToolNameStrategy，而不是依赖真实模型是否愿意生成未知工具名。
     */
    private String runHallucinatedToolTest01(String toolName) {
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("测试工具名不能为空，例如：__hallucinated_tool_test__:getWeather");
        }

        ChatModel testChatModel = new ChatModel() {
            private boolean firstResponse = true;

            @Override
            public ChatResponse doChat(ChatRequest request) {
                if (firstResponse) {
                    firstResponse = false;
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .id("hallucinated-test-call")
                            .name(toolName)
                            .arguments("{}")
                            .build();
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from(toolExecutionRequest))
                            .build();
                }

                // 已触发工具幻觉处理策略：工具 'getWeather' 不存在或当前不可用。只能使用已提供的预约工具，请根据用户需求重新选择工具。
                ToolExecutionResultMessage resultMessage = request.messages().stream()
                        .filter(ToolExecutionResultMessage.class::isInstance)
                        .map(ToolExecutionResultMessage.class::cast)
                        .filter(result -> toolName.equals(result.toolName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未收到工具幻觉处理策略返回的工具执行结果"));
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("已触发工具幻觉处理策略：" + resultMessage.text()))
                        .build();
            }
        };

        Assistant assistant = buildAssistant(testChatModel);
        Result<String> chatResult = assistant.chat("执行工具幻觉处理测试");
        log.info("Tool Executions: {}", chatResult.toolExecutions());
        return chatResult.content();
    }

    /**
     * 当模型请求了未注册的工具时，将未知工具信息作为工具执行结果返回给模型，
     * 让模型自行修正，而不是让整个对话直接抛异常结束。
     */
    private ToolExecutionResultMessage handleHallucinatedTool(ToolExecutionRequest request) {
        log.warn("模型请求了未注册的工具, toolName: {}, arguments: {}", request.name(), request.arguments());

        String result = "工具 '%s' 不存在或当前不可用。只能使用已提供的预约工具，请根据用户需求重新选择工具。"
                .formatted(request.name());
        return ToolExecutionResultMessage.from(request, result);
    }
}
