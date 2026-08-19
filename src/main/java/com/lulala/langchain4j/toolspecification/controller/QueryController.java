package com.lulala.langchain4j.toolspecification.controller;

import com.lulala.langchain4j.toolspecification.service.QueryService;
import com.lulala.langchain4j.toolspecification.tools.QueryTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 参见：https://langchain4j.cn/tutorials/tools.html<br/>
 * 高级工具 API
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 15:47
 */
@Slf4j
@RestController
@RequestMapping("/toolspecification/query")
public class QueryController {

    @Autowired
    private ChatModel deepseekChatModel;
    @Autowired
    private QueryTools queryTools;

    /**
     * 根据查询内容返回相关的 URL 列表
     * @param query
     * @return java.util.List<java.lang.String>
     * @author shenjh
     * @since 2026/8/19 15:52
     */
    @RequestMapping("/getUrlsByQueryContent")
    public List<String> getUrlsByQueryContent(@RequestParam("query") String query) {
        QueryService queryService = AiServices.builder(QueryService.class)
                .chatModel(deepseekChatModel)
                .tools(queryTools)
                .build();
        return queryService.getUrlsByQueryContent(query);
    }

    /**
     * 根据给定的 URL 获取对应网页的内容
     * @param url
     * @return java.util.List<java.lang.String>
     * @author shenjh
     * @since 2026/8/19 15:52
     */
    @RequestMapping("/getWebPageByUrl")
    public String getWebPageByUrl(@RequestParam("url") String url) {
        QueryService queryService = AiServices.builder(QueryService.class)
                .chatModel(deepseekChatModel)
                .tools(queryTools)
                .build();
        return queryService.getWebPageByUrl(url);
    }

}
