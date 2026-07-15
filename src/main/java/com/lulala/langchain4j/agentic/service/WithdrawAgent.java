package com.lulala.langchain4j.agentic.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/7/15 16:58
 */
public interface WithdrawAgent {

    /*
        如果只在 @Agent 添加描述，去掉 @SystemMessage 注解，会怎么样？
        <pre>
            直接调用 withdraw 时，@Agent 描述基本不会约束模型行为。
            模型不再明确知道自己要扮演“银行柜员”。
            “仅支持 USD”这种规则不再是 system 级约束，只能靠 @UserMessage 里的“美元”间接表达。
            在 supervisor/router 场景中，@Agent 描述仍然有用，因为上层可以根据它决定是否调用这个 agent。
        </pre>
     */
    /*
        已经有了 @SystemMessage 的描述，@Agent 加不加描述有什么区别？
        <pre>
            如果直接调用 withdrawAgent.withdraw("张三", 100.0)，@Agent 描述基本不影响模型回答，真正影响回答的是 @SystemMessage + @UserMessage。
            如果 WithdrawAgent 被放进 sequenceBuilder、conditionalBuilder、supervisorBuilder 等 agentic workflow 中，@Agent 描述就有意义。上层 agent/planner 可能会根据这个描述理解它的职责。
            如果不写 @Agent 描述，只写 @Agent，它仍然是一个 agent，默认名称是方法名 withdraw，但描述为空。对于固定顺序 workflow 问题不大；对于 supervisor/router 这种需要“理解哪个 agent 适合做什么”的场景，效果会变差。
        </pre>
     */
    // 控制 LLM 怎么思考/扮演什么角色/遵守什么规则，参与构造最终发给 ChatModel 的消息，影响模型如何回答
    @SystemMessage("""
            你是一名银行柜员，仅支持从用户账户中支取美元（USD）。
            """)
    @UserMessage("""
            从 {{user}} 的账户中提取 {{amount}} 美元，并返回最新余额。
            """)
    // 控制 LangChain4j 把这个方法当作什么 Agent 来编排，告诉 LangChain4j 这个智能体是用来干什么的
    @Agent("负责从账户中支取美元的银行柜员")
    String withdraw(@V("user") String user, @V("amount") Double amount);
}
