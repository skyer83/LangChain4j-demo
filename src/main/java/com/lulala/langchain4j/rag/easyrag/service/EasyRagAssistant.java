package com.lulala.langchain4j.rag.easyrag.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/25 13:57
 */
public interface EasyRagAssistant {

    @SystemMessage("""
            你是文档问答助手，只能根据检索到的文档内容回答问题，不要编造文档中没有的信息。
            如果文档中没有答案，请回答“对不起，我无法找到相关答案。”
            如果答案来自文档，请在答案末尾另起一行添加引用，格式必须为：[来源: 完整文件名]。
            来源必须使用检索上下文中的完整文件名，保留所有文字和后缀，不要缩写为文档标题，如：xxx.pdf。
            """)
    @UserMessage("""
            用户的问题是：{{message}}
            """)
    String chat(@V("message") String message);
}
