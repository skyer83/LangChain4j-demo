package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.service.CreditAgent;
import com.lulala.langchain4j.agentic.service.ExchangeAgent;
import com.lulala.langchain4j.agentic.service.WithdrawAgent;
import com.lulala.langchain4j.agentic.tools.BankTool;
import com.lulala.langchain4j.agentic.tools.ExchangeTool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 纯代理式 AI
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 16:55
 */
@RestController("/bank")
public class BankController {

    @Autowired
    private ChatModel gptChatModel;

    @GetMapping("/exchange")
    public String exchange() {
        BankTool bankTool = new BankTool();
        bankTool.createAccount("张三", 1000.0);
        bankTool.createAccount("李四", 1000.0);

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(gptChatModel)
                .tools(bankTool)
                .build();

        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(gptChatModel)
                .tools(bankTool)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(gptChatModel)
                .tools(new ExchangeTool())
                .build();

        SupervisorAgent supervisorAgent = AgenticServices
                .supervisorBuilder()
                .chatModel(gptChatModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                // SupervisorResponseStrategy.LAST，返回最后一个被调用子 agent 的输出，默认策略。
                // SupervisorResponseStrategy.SUMMARY，返回 supervisor 对整个执行过程的总结。
                // SupervisorResponseStrategy.SCORED，让内部 LLM 比较 LAST 和 SUMMARY 哪个更符合用户请求，然后返回得分更高的那个。
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        return supervisorAgent.invoke("从张三账户向李四账户转账 100 欧元。");
    }
}
