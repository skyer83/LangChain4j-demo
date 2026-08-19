package com.lulala.langchain4j.toolspecification.tools;

import com.lulala.langchain4j.toolspecification.service.IBaiduSearchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 15:54
 */
@Slf4j
@Component
public class QueryTools {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final IBaiduSearchService baiduSearchService;

    public QueryTools(IBaiduSearchService baiduSearchService) {
        this.baiduSearchService = baiduSearchService;
    }

    @Tool("根据给定的查询条件，在百度上搜索相关的 URL")
    public List<String> searchBaidu(@P("查询条件") String query) {
        return baiduSearchService.search(query);
    }

    @Tool("根据给定的 URL，返回网页的内容")
    public String getWebPageContent(@P("给定的 URL") String url) {
        if (url == null || url.isBlank()) {
            return "URL 不能为空";
        }

        try {
            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15_000)
                    .get();
            return document.body() == null ? "" : document.body().text();
        } catch (Exception e) {
            log.error("获取网页内容异常", e);
            return "获取网页内容失败：" + e.getMessage();
        }
    }

}
