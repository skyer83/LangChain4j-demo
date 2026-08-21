package com.lulala.langchain4j.toolspecification.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/21 10:17
 */
public interface Assistant {

    @UserMessage("""
            你是一个预约助手，请根据用户的要求处理预约相关的问题，并返回处理结果信息。
            用户的要求是：{{message}}
            """)
    Result<String> chat(@V("message") String message);
}
