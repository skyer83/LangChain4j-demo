package com.lulala.langchain4j.common;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/27 14:56
 */
public class Constants {

    /** 系统环境变量KEY， 加了环境变量后，IDEA 工具要完全重启才能取到环境变量的值 */
    public static final class SystemEnv {
        /** RAG 示例文档 */
        public static final String LANGCHAIN4J_DEMO_DOC_PATH = "LANGCHAIN4J_DEMO_DOC_PATH";

        // get a free key: https://app.tavily.com/sign-in
        /** Tavily 是一个专为 AI 代理（AI Agents） 和 大语言模型（LLMs） 设计的搜索引擎 API */
        public static final String LANGCHAIN4J_TAVILY_API_KEY = "LANGCHAIN4J_TAVILY_API_KEY";

        // https://dashboard.cohere.com/api-keys
        /** 重排序（Rerank）/语义搜索（Semantic Search）的 API */
        public static final String LANGCHAIN4J_COHERE_API_KEY = "LANGCHAIN4J_COHERE_API_KEY";

        // https://jina.ai/
        /** Jina Reranker 的 API（免费API密钥，不是固定的） */
        public static final String LANGCHAIN4J_JINA_API_KEY = "LANGCHAIN4J_JINA_API_KEY";

        // https://bailian.console.aliyun.com/
        /** DashScope/Qwen Reranker 的 API */
        public static final String LANGCHAIN4J_DASHSCOPE_API_KEY = "LANGCHAIN4J_DASHSCOPE_API_KEY";

    }
}
