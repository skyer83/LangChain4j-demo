package com.lulala.langchain4j.toolspecification.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulala.langchain4j.toolspecification.service.IBaiduSearchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 15:54
 */
@Slf4j
@Component
public class QueryUrlTools {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36";
    private static final int MAX_CONTENT_LENGTH = 8_000;
    private static final int MIN_READABLE_TEXT_LENGTH = 40;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> CHROME_EXECUTABLE_CANDIDATES = List.of(
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
            "/usr/bin/google-chrome",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/microsoft-edge"
    );

    private final IBaiduSearchService baiduSearchService;

    public QueryUrlTools(IBaiduSearchService baiduSearchService) {
        this.baiduSearchService = baiduSearchService;
    }

    @Tool("根据给定的查询条件，在百度上搜索相关的 URL")
    public List<String> searchBaidu(@P("查询条件") String query) {
        return baiduSearchService.search(query);
    }

    @Tool("根据给定的 URL，模拟浏览器请求并返回网页内容信息")
    public String getWebPageContent(@P("给定的 URL") String url) {
        if (url == null || url.isBlank()) {
            return "URL 不能为空";
        }

        String normalizedUrl = normalizeUrl(url);
        FetchResult directResult = fetchPage(normalizedUrl, "原始 URL");
        if (directResult.isReadable()) {
            return directResult.format();
        }

        FetchResult browserResult = fetchWithLocalBrowser(normalizedUrl);
        if (browserResult.isReadable()) {
            return browserResult.format();
        }

        FetchResult lastResult = browserResult;
        for (String readerUrl : readerUrls(normalizedUrl)) {
            FetchResult readerResult = fetchPage(readerUrl, "阅读器降级");
            if (readerResult.isReadable()) {
                return readerResult.withSourceUrl(normalizedUrl).format();
            }
            lastResult = readerResult;
        }

        return lastResult.withSourceUrl(normalizedUrl).formatFailure(normalizedUrl);
    }

    private FetchResult fetchPage(String url, String strategy) {
        try {
            Connection.Response response = browserLikeConnection(url).execute();
            int statusCode = response.statusCode();
            String statusMessage = response.statusMessage();
            String contentType = response.contentType();
            String finalUrl = response.url() == null ? url : response.url().toString();

            if (statusCode < 200 || statusCode >= 300) {
                return FetchResult.failure(strategy, finalUrl, statusCode, statusMessage, contentType, "");
            }

            if (isJsonFetchFailure(response, contentType)) {
                return FetchResult.failure(strategy, finalUrl, statusCode, statusMessage, contentType, response.body());
            }

            String text = extractText(response, contentType);
            if (isBlockedOrEmptyPage(text)) {
                return FetchResult.failure(strategy, finalUrl, statusCode, statusMessage, contentType, text);
            }

            String title = extractTitle(response, contentType);
            return FetchResult.success(strategy, finalUrl, statusCode, statusMessage, contentType, title, text);
        } catch (Exception e) {
            log.debug("获取网页内容失败, url: {}, error: {}", url, e.getMessage());
            return FetchResult.failure(strategy, url, 0, e.getMessage(), null, "");
        }
    }

    private FetchResult fetchWithLocalBrowser(String url) {
        String browserExecutable = findBrowserExecutable();
        if (browserExecutable == null) {
            return FetchResult.failure("本机浏览器降级", url, 0, "未找到 Chrome/Edge 可执行文件", null, "");
        }

        List<Path> outputFiles = new ArrayList<>();
        Path userDataDir = null;
        try {
            userDataDir = Files.createTempDirectory("web-browser-profile-");

            FetchResult lastResult = FetchResult.failure("本机浏览器降级", url, 0, "浏览器未输出 DOM", null, "");
            for (int attempt = 1; attempt <= 2; attempt++) {
                Path outputFile = Files.createTempFile("web-page-", ".html");
                outputFiles.add(outputFile);
                outputFile.toFile().deleteOnExit();

                List<String> command = new ArrayList<>();
                command.add(browserExecutable);
                command.add("--headless=new");
                command.add("--disable-gpu");
                command.add("--no-sandbox");
                command.add("--disable-dev-shm-usage");
                command.add("--disable-blink-features=AutomationControlled");
                command.add("--window-size=1365,900");
                command.add("--user-data-dir=" + userDataDir.toAbsolutePath());
                command.add("--user-agent=" + USER_AGENT);
                command.add("--virtual-time-budget=" + (attempt == 1 ? "12000" : "15000"));
                command.add("--dump-dom");
                command.add(url);

                Process process = new ProcessBuilder(command)
                        .redirectOutput(outputFile.toFile())
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                boolean finished = process.waitFor(50, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                    lastResult = extractBrowserRenderedResultOrFailure(url, outputFile, "浏览器渲染超时");
                } else if (process.exitValue() != 0) {
                    lastResult = extractBrowserRenderedResultOrFailure(url, outputFile, "浏览器退出码: " + process.exitValue());
                } else {
                    lastResult = extractBrowserRenderedResultOrFailure(url, outputFile, "浏览器未输出 DOM");
                }

                if (lastResult.isReadable()) {
                    return lastResult;
                }
            }

            return lastResult;
        } catch (Exception e) {
            log.warn("本机浏览器渲染网页失败, url: {}", url, e);
            return FetchResult.failure("本机浏览器降级", url, 0, e.getMessage(), null, "");
        } finally {
            for (Path outputFile : outputFiles) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (Exception e) {
                    log.debug("删除浏览器临时输出文件失败: {}", outputFile, e);
                }
            }
            if (userDataDir != null) {
                deleteDirectory(userDataDir);
            }
        }
    }

    private FetchResult extractBrowserRenderedResultOrFailure(String url, Path outputFile, String fallbackMessage) {
        try {
            if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
                return extractBrowserRenderedResult(url, outputFile);
            }
            return FetchResult.failure("本机浏览器降级", url, 0, fallbackMessage, null, "");
        } catch (Exception e) {
            log.debug("读取浏览器渲染结果失败, outputFile: {}, error: {}", outputFile, e.getMessage());
            return FetchResult.failure("本机浏览器降级", url, 0, e.getMessage(), null, "");
        }
    }

    private void deleteDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.debug("删除浏览器临时目录失败: {}", path, e);
                        }
                    });
        } catch (Exception e) {
            log.debug("遍历浏览器临时目录失败: {}", directory, e);
        }
    }

    private FetchResult extractBrowserRenderedResult(String url, Path outputFile) throws Exception {
        if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
            return FetchResult.failure("本机浏览器降级", url, 0, "浏览器未输出 DOM", null, "");
        }

        String html = Files.readString(outputFile, StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html, url);
        document.select("script, style, noscript, svg, canvas, iframe, nav, footer, .Modal-wrapper, .signFlowModal").remove();

        String title = document.title();
        String text = extractReadableText(document);
        if (isBlockedOrEmptyPage(text)) {
            return FetchResult.failure("本机浏览器降级", url, 200, "OK", "text/html; rendered=chrome", text);
        }

        return FetchResult.success("本机浏览器降级", url, 200, "OK", "text/html; rendered=chrome", title, text);
    }

    private String findBrowserExecutable() {
        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin != null && !chromeBin.isBlank() && new File(chromeBin).exists()) {
            return chromeBin;
        }

        for (String candidate : CHROME_EXECUTABLE_CANDIDATES) {
            if (new File(candidate).exists()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isJsonFetchFailure(Connection.Response response, String contentType) {
        try {
            String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
            if (!lowerContentType.contains("json")) {
                return false;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            int upstreamStatusCode = root.path("statusCode").asInt(200);
            JsonNode data = root.path("data");
            boolean noReadableData = data.path("markdown").isMissingNode()
                    || data.path("markdown").isNull()
                    || data.path("markdown").asText("").isBlank();
            return upstreamStatusCode >= 400 && noReadableData;
        } catch (Exception e) {
            log.debug("判断 JSON 抓取结果失败", e);
            return false;
        }
    }

    private String extractText(Connection.Response response, String contentType) throws Exception {
        String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String body = response.body();

        if (lowerContentType.contains("json")) {
            String markdown = extractMarkdownFromJson(body);
            if (!markdown.isBlank()) {
                return markdown;
            }
            return body == null ? "" : body;
        }

        if (!lowerContentType.contains("html")) {
            return body == null ? "" : body;
        }

        Document document = response.parse();
        document.select("script, style, noscript, svg, canvas, iframe, nav, footer").remove();

        return extractReadableText(document);
    }

    private String extractReadableText(Document document) {
        Element main = document.selectFirst(String.join(", ",
                "article",
                "main",
                "[role=main]",
                "[itemprop=articleBody]",
                ".article",
                ".article-content",
                ".article-body",
                ".article__content",
                ".entry-content",
                ".post-content",
                ".post-body",
                ".content",
                ".main-content",
                ".markdown-body",
                ".rich-text",
                ".RichText",
                ".RichContent",
                ".Post-RichText",
                "#article",
                "#content",
                "#main"
        ));
        String text = main == null ? "" : main.text();
        if (text.length() >= MIN_READABLE_TEXT_LENGTH) {
            return text;
        }

        String descriptionText = extractMetaDescription(document);
        if (!descriptionText.isBlank()) {
            return descriptionText;
        }

        return document.body() == null ? "" : document.body().text();
    }

    private String extractMetaDescription(Document document) {
        Element description = document.selectFirst("meta[name=description], meta[property=og:description]");
        return description == null ? "" : description.attr("content");
    }

    private String extractTitle(Connection.Response response, String contentType) {
        try {
            String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
            if (lowerContentType.contains("html")) {
                return response.parse().title();
            }
            if (lowerContentType.contains("json")) {
                JsonNode root = OBJECT_MAPPER.readTree(response.body());
                JsonNode title = root.path("data").path("title");
                return title.isMissingNode() ? root.path("title").asText("") : title.asText("");
            }
        } catch (Exception e) {
            log.debug("提取网页标题失败", e);
        }
        return "";
    }

    private String extractMarkdownFromJson(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return "";
        }
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode data = root.path("data");
        JsonNode markdown = data.path("markdown");
        if (!markdown.isMissingNode() && !markdown.isNull() && !markdown.asText("").isBlank()) {
            return markdown.asText();
        }
        JsonNode description = data.path("description");
        if (!description.isMissingNode() && !description.isNull() && !description.asText("").isBlank()) {
            return description.asText();
        }
        return "";
    }

    private boolean isBlockedOrEmptyPage(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        String compactText = text.replaceAll("\\s+", "");
        if (compactText.length() < MIN_READABLE_TEXT_LENGTH) {
            return true;
        }

        return compactText.contains("您当前请求存在异常")
                || compactText.contains("暂时限制本次访问")
                || compactText.contains("安全验证")
                || compactText.contains("访问异常")
                || compactText.contains("请完成安全验证")
                || compactText.contains("人机验证")
                || compactText.contains("验证您是真人")
                || compactText.contains("登录后查看更多")
                || compactText.contains("请登录")
                || compactText.toLowerCase(Locale.ROOT).contains("accessdenied")
                || compactText.toLowerCase(Locale.ROOT).contains("forbidden")
                || compactText.toLowerCase(Locale.ROOT).contains("enablejavascript")
                || compactText.toLowerCase(Locale.ROOT).contains("captcha")
                || compactText.matches(".*知乎[-—]?500.*");
    }

    private List<String> readerUrls(String originalUrl) {
        String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
        return List.of(
                "https://r.jina.ai/" + originalUrl,
                "https://api.microlink.io/?url=" + encodedUrl + "&data.markdown.attr=markdown"
        );
    }

    private String normalizeUrl(String url) {
        return url.trim()
                .replace("https: //", "https://")
                .replace("http: //", "http://")
                .replace("https:/ /", "https://")
                .replace("http:/ /", "http://");
    }

    private Connection browserLikeConnection(String url) {
        return Jsoup.connect(url)
                .method(Connection.Method.GET)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0)
                .timeout(30_000);
    }

    private static String limitText(String text) {
        String normalizedText = text.replaceAll("\\s+", " ").trim();
        if (normalizedText.length() <= MAX_CONTENT_LENGTH) {
            return normalizedText;
        }
        return normalizedText.substring(0, MAX_CONTENT_LENGTH) + "...";
    }

    private record FetchResult(
            boolean success,
            String strategy,
            String sourceUrl,
            String finalUrl,
            int statusCode,
            String statusMessage,
            String contentType,
            String title,
            String text
    ) {

        private static FetchResult success(
                String strategy,
                String finalUrl,
                int statusCode,
                String statusMessage,
                String contentType,
                String title,
                String text
        ) {
            return new FetchResult(true, strategy, finalUrl, finalUrl, statusCode, statusMessage, contentType, title, text);
        }

        private static FetchResult failure(
                String strategy,
                String finalUrl,
                int statusCode,
                String statusMessage,
                String contentType,
                String text
        ) {
            return new FetchResult(false, strategy, finalUrl, finalUrl, statusCode, statusMessage, contentType, "", text);
        }

        private boolean isReadable() {
            return success;
        }

        private FetchResult withSourceUrl(String sourceUrl) {
            return new FetchResult(success, strategy, sourceUrl, finalUrl, statusCode, statusMessage, contentType, title, text);
        }

        private String format() {
            StringBuilder result = new StringBuilder();
            result.append("获取方式: ").append(strategy).append('\n');
            result.append("URL: ").append(sourceUrl).append('\n');
            if (!sourceUrl.equals(finalUrl)) {
                result.append("返回地址: ").append(finalUrl).append('\n');
            }
            result.append("HTTP 状态码: ").append(statusCode);
            if (statusMessage != null && !statusMessage.isBlank()) {
                result.append(' ').append(statusMessage);
            }
            result.append('\n');
            if (contentType != null && !contentType.isBlank()) {
                result.append("Content-Type: ").append(contentType).append('\n');
            }
            if (title != null && !title.isBlank()) {
                result.append("标题: ").append(title).append('\n');
            }
            result.append("正文: ").append(text == null ? "" : limitText(text));
            return result.toString();
        }

        private String formatFailure(String originalUrl) {
            StringBuilder result = new StringBuilder();
            result.append("获取网页内容失败: 未获取到可读正文").append('\n');
            result.append("URL: ").append(originalUrl).append('\n');
            result.append("最后尝试方式: ").append(strategy).append('\n');
            if (statusCode > 0) {
                result.append("最后 HTTP 状态码: ").append(statusCode);
                if (statusMessage != null && !statusMessage.isBlank()) {
                    result.append(' ').append(statusMessage);
                }
                result.append('\n');
            } else if (statusMessage != null && !statusMessage.isBlank()) {
                result.append("最后错误: ").append(statusMessage).append('\n');
            }
            if (text != null && !text.isBlank()) {
                result.append("返回摘要: ").append(limitText(text));
            }
            return result.toString();
        }
    }
}
