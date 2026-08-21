package com.lulala.langchain4j.toolspecification.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/20 17:36
 */
public interface RouterAgent {

    /**
     * 调用专家服务
     * @param request
     * @return
     */
    @UserMessage("""
            分析以下用户请求，将其归类为“法律”、“医疗”或“技术”，然后将该请求原样转发给作为工具提供的相应领域专家。
            最后，原封不动地返回您从专家处收到的答复。
            用户请求是：“{{request}}”。
            """)
    String askToExpert(@V("request") String request);
}
