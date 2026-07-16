package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.*;
import com.lulala.langchain4j.agentic.tools.BankTool;
import com.lulala.langchain4j.agentic.tools.BankToolOfBigDecimal;
import com.lulala.langchain4j.agentic.tools.ExchangeTool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 纯代理式 AI、非 AI 代理
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 16:55
 */
@RestController
@RequestMapping("/bank")
public class BankController {

    @Autowired
    private ChatModel gptChatModel;

    /**
     * 构建时配置
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/16 14:19
     */
    @GetMapping("/exchange")
    public String exchange() {
        ChatModel chatModel = gptChatModel;

        BankTool bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(chatModel)
                .tools(new ExchangeTool())
                .build();

        SupervisorAgent supervisorAgent = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .supervisorContext("执行策略：优先调用内部工具；统一使用美元（USD）；严禁调用外部API。")
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                // SupervisorResponseStrategy.LAST，返回最后一个被调用子 agent 的输出，默认策略。
                // SupervisorResponseStrategy.SUMMARY，返回 supervisor 对整个执行过程的总结。
                // SupervisorResponseStrategy.SCORED，让内部 LLM 比较 LAST 和 SUMMARY 哪个更符合用户请求，然后返回得分更高的那个。
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        return supervisorAgent.invoke("从张三账户向李四账户转账 100 欧元。");
    }
    
    /**
     * 调用时（类型化的监督代理）
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/16 14:17
     */
    @GetMapping("/myExchange01")
    public String myExchange01() {
        ChatModel chatModel = gptChatModel;

        BankTool bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(chatModel)
                .tools(new ExchangeTool())
                .build();

        MySupervisorAgent01 supervisorAgent = AgenticServices
                .supervisorBuilder(MySupervisorAgent01.class)
                .chatModel(chatModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                // SupervisorResponseStrategy.LAST，返回最后一个被调用子 agent 的输出，默认策略。
                // SupervisorResponseStrategy.SUMMARY，返回 supervisor 对整个执行过程的总结。
                // SupervisorResponseStrategy.SCORED，让内部 LLM 比较 LAST 和 SUMMARY 哪个更符合用户请求，然后返回得分更高的那个。
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
        // 如果两种方式（构建时也设置了supervisorContext）都提供了，调用时的值会覆盖构建时的值
        return supervisorAgent.invoke("从张三账户向李四账户转账 100 欧元。", "执行策略：必须先转换为美元；仅限使用银行工具；严禁调用外部API。");
    }

    /**
     * 调用时（非类型化的监督代理）
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/7/16 14:17
     */
    @GetMapping("/myExchange02")
    public String myExchange02() {
        ChatModel chatModel = gptChatModel;

        BankTool bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(chatModel)
                .tools(new ExchangeTool())
                .build();

        UntypedAgent supervisorAgent = AgenticServices
                .supervisorBuilder(UntypedAgent.class)
                .chatModel(chatModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                // SupervisorResponseStrategy.LAST，返回最后一个被调用子 agent 的输出，默认策略。
                // SupervisorResponseStrategy.SUMMARY，返回 supervisor 对整个执行过程的总结。
                // SupervisorResponseStrategy.SCORED，让内部 LLM 比较 LAST 和 SUMMARY 哪个更符合用户请求，然后返回得分更高的那个。
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 如果两种方式（构建时也设置了supervisorContext）都提供了，调用时的值会覆盖构建时的值
        Map<String, Object> requestMap = Map.of(
                "request", "从张三账户向李四账户转账 100 欧元。",
                "supervisorContext", "执行策略：必须先转换为美元；仅限使用银行工具；严禁调用外部API。"
        );
        return (String) supervisorAgent.invoke(requestMap);
    }

    /**
     * 非 AI 代理
     * @return java.lang.String
     * @author shenjh
     * @since 2026/7/16 14:17
     */
    @GetMapping("/noAiAgentExchange")
    public String noAiAgentExchange() {
        ChatModel chatModel = gptChatModel;

        BankToolOfBigDecimal bankTool = new BankToolOfBigDecimal();
        bankTool.createAccount("张三", BigDecimal.valueOf(1000.0));
        bankTool.createAccount("李四", BigDecimal.valueOf(1000.0));

        WithdrawAgentOfBigDecimal withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgentOfBigDecimal.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        CreditAgentOfBigDecimal creditAgent = AgenticServices
                .agentBuilder(CreditAgentOfBigDecimal.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();

        UntypedAgent supervisorAgent = AgenticServices
                .supervisorBuilder(UntypedAgent.class)
                .chatModel(chatModel)
                .subAgents(withdrawAgent, creditAgent, new ExchangeOperator())
                // SupervisorResponseStrategy.LAST，返回最后一个被调用子 agent 的输出，默认策略。
                // SupervisorResponseStrategy.SUMMARY，返回 supervisor 对整个执行过程的总结。
                // SupervisorResponseStrategy.SCORED，让内部 LLM 比较 LAST 和 SUMMARY 哪个更符合用户请求，然后返回得分更高的那个。
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 如果两种方式（构建时也设置了supervisorContext）都提供了，调用时的值会覆盖构建时的值
        Map<String, Object> requestMap = Map.of(
                "request", "从张三账户向李四账户转账 100 欧元。",
                "supervisorContext", "执行策略：必须先转换为美元；仅限使用银行工具；严禁调用外部API。"
        );
        return (String) supervisorAgent.invoke(requestMap);
    }
}
