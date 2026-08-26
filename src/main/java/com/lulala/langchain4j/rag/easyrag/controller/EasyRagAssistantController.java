package com.lulala.langchain4j.rag.easyrag.controller;

import cn.hutool.core.util.StrUtil;
import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import com.lulala.langchain4j.rag.easyrag.service.EasyRagAssistant;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;

/**
 * 参见：https://langchain4j.cn/tutorials/rag.html#easy-rag<br/>
 * Easy RAG
 * @author shenjh
 * @version 1.0
 * @since 2026/8/25 14:32
 */
@RestController
@RequestMapping("/rag/assistant")
public class EasyRagAssistantController {

    private final EasyRagAssistant assistant;
    private final ContentRetriever contentRetriever;

    public EasyRagAssistantController(@Qualifier(LangChain4JConstants.ChatModel.DEEPSEEK_CHAT_MODEL) ChatModel deepseekChatModel,
                                      EmbeddingStore<TextSegment> embeddingStore,
                                      EmbeddingModel embeddingModel) {
        this.contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.25)
                .build();

        // 助手是无状态单例；聊天记忆需要按用户或会话隔离，不能放在全局控制器中共享。
        this.assistant = AiServices.builder(EasyRagAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
                .build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        String answer = assistant.chat(message);
        String sourceFileName = resolveSourceFileName(message);
        return normalizeSource(answer, sourceFileName);
    }

    private String resolveSourceFileName(String message) {
        return contentRetriever.retrieve(new Query(message)).stream()
                .map(content -> content.textSegment().metadata().getString(Document.FILE_NAME))
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private String normalizeSource(String answer, String sourceFileName) {
        if (StrUtil.isBlank(sourceFileName)) {
            return answer;
        }

        // 将简写的来源替换为完整的文件名，如：[来源: EagleTrader Max] 替换为 [来源: EagleTrader Max 规则补充条款.pdf]
        String citation = "[来源: " + sourceFileName + "]";
        if (answer.contains("[来源:")) {
            return answer.replaceAll("\\[来源:\\s*[^\\]]+\\]", Matcher.quoteReplacement(citation));
        }
        return answer + System.lineSeparator() + citation;
    }
}
