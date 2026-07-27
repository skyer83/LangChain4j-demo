package com.lulala.langchain4j.openai.constant;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 9:42
 */
public class LangChain4JConstants {

    /** AI模型 */
    public static final class ChatModel {
        /** 对应配置项：langchain4j.open-ai.chat-model */
        public static final String OPEN_AI_CHAT_MODEL = "openAiChatModel";
        /** 对应配置项：langchain4j.open-ai.streaming-chat-model */
        public static final String OPEN_AI_STREAMING_CHAT_MODEL = "openAiStreamingChatModel";
        /** 对应配置项：langchain4j.ollama.chat-model */
        public static final String OLLAMA_CHAT_MODEL = "ollamaChatModel";
        /** 对应配置项：langchain4j.custom.chat-model.gpt */
        public static final String GPT_CHAT_MODEL = "gptChatModel";
        /** 对应配置项：langchain4j.custom.chat-model.deepseek */
        public static final String DEEPSEEK_CHAT_MODEL = "deepseekChatModel";
    }
}
