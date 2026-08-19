package com.lulala.langchain4j.toolspecification.service.impl;

import com.lulala.langchain4j.toolspecification.service.IBaiduSearchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 16:22
 */
@Slf4j
@Service
public class BaiduSearchServiceImpl implements IBaiduSearchService {

    private static final String BAIDU_SEARCH_URL = "https://www.baidu.com/s?wd=%s";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final int MAX_SEARCH_RESULTS = 5;

    @Override
    public List<String> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            Document document = Jsoup.connect(BAIDU_SEARCH_URL.formatted(encodedQuery))
                    .userAgent(USER_AGENT)
                    .referrer("https://www.baidu.com/")
                    .timeout(15_000)
                    .get();

            Set<String> urls = new LinkedHashSet<>();
            Elements resultLinks = document.select("h3 a[href], .result a[href], .c-container a[href]");
            for (Element resultLink : resultLinks) {
                String url = resultLink.absUrl("href");
                if (isSearchResultUrl(url)) {
                    urls.add(url);
                }
                if (urls.size() >= MAX_SEARCH_RESULTS) {
                    break;
                }
            }
            return new ArrayList<>(urls);
        } catch (SocketTimeoutException e) {
            log.error("百度搜索超时", e);
            return List.of("百度搜索超时，请稍后重试");
        } catch (Exception e) {
            log.error("百度搜索异常", e);
            return List.of("百度搜索失败：" + e.getMessage());
        }
    }

    private boolean isSearchResultUrl(String url) {
        return (url.startsWith("http://") || url.startsWith("https://"))
                && (!url.contains("baidu.com") || url.contains("baidu.com/link?url="));
    }
}
