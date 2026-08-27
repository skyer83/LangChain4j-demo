package com.lulala.langchain4j.rag.ragapi.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/25 13:57
 */
public interface WebAssistant {

    @SystemMessage("""
            你是网络问答助手，只能根据网络搜索到的内容回答用户的问题，不要编造内容中没有的信息。
            如果网络内容中没有答案，请回答“对不起，我无法找到相关答案。”
            如果答案来自网络内容，请在答案末尾另起一行添加引用，格式必须为：[来源: [网站标题](完整网络链接地址)]。
            """)
    @UserMessage("""
            用户的问题是：{{message}}
            """)
    String chat(@V("message") String message);
}
