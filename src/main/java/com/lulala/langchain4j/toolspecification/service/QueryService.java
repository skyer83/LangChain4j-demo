package com.lulala.langchain4j.toolspecification.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 15:59
 */
public interface QueryService {

    @UserMessage("根据查询内容返回相关的 URL 列表。查询内容：{{queryContent}}")
    List<String> getUrlsByQueryContent(@V("queryContent") String queryContent);

    @UserMessage("根据给定的 URL 获取对应网页的内容。URL：{{url}}")
    String getWebPageByUrl(@V("url") String url);
}
