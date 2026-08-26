package com.lulala.langchain4j.rag.easyrag.controller;

import cn.hutool.core.util.StrUtil;
import com.lulala.langchain4j.openai.constant.LangChain4JConstants;
import com.lulala.langchain4j.rag.easyrag.config.EasyRagConfig;
import com.lulala.langchain4j.rag.easyrag.service.EasyRagAssistant;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;

/**
 * 参见：https://langchain4j.cn/tutorials/rag.html#easy-rag<br/>
 * Easy RAG
 * @author shenjh
 * @version 1.0
 * @since 2026/8/25 14:32
 */
@Slf4j
@RestController
@RequestMapping("/rag/assistant")
public class EasyRagAssistantController {

    private final EasyRagAssistant assistant4EasyRag;
    private final ContentRetriever contentRetriever4EasyRag;

    private final EasyRagAssistant assistant4Transformer;
    private final ContentRetriever contentRetriever4Transformer;

    private final EasyRagAssistant assistant4Embedding;

    public EasyRagAssistantController(@Qualifier(LangChain4JConstants.ChatModel.DEEPSEEK_CHAT_MODEL) ChatModel deepseekChatModel,
                                      @Qualifier(EasyRagConfig.BeanName.CONTENT_RETRIEVER_4_EASY_RAG) ContentRetriever contentRetriever4EasyRag,
                                      @Qualifier(EasyRagConfig.BeanName.CONTENT_RETRIEVER_4_TRANSFORMER) ContentRetriever contentRetriever4Transformer,
                                      @Qualifier(EasyRagConfig.BeanName.CONTENT_RETRIEVER_4_EMBEDDING) ContentRetriever contentRetriever4Embedding) {
        this.contentRetriever4EasyRag = contentRetriever4EasyRag;
        this.assistant4EasyRag = AiServices.builder(EasyRagAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever4EasyRag)
                .build();

        this.contentRetriever4Transformer = contentRetriever4Transformer;
        this.assistant4Transformer = AiServices.builder(EasyRagAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever4Transformer)
                .build();

        this.assistant4Embedding = AiServices.builder(EasyRagAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever4Embedding)
                .build();
    }

    @GetMapping("/chat4EasyRag")
    public String chat4EasyRag(@RequestParam("message") String message) {
        String answer = assistant4EasyRag.chat(message);
        String sourceFileName = resolveSourceFileName4EasyRag(message);
        return normalizeSource(answer, sourceFileName);
    }

    private String resolveSourceFileName4EasyRag(String message) {
        List<Content> contentList = contentRetriever4EasyRag.retrieve(new Query(message));
        // resolveSourceFileName4EasyRag contentList: [DefaultContent { textSegment = TextSegment { text = "拆分后的文本内容" metadata = {absolute_directory_path=C:\systemEnv\xxx\1, index=1, file_name=xxx.pdf} }, metadata = {SCORE=0.8564665629710392, EMBEDDING_ID=f6778b9a-ec6f-45cb-bc5e-b91b182b36ea} }]
        //log.info("resolveSourceFileName4EasyRag contentList: {}", contentList);
        return contentList.stream()
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
    
    /**
     * 嵌入存储摄取器（Embedding Store Ingestor）
     * @param message
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/8/26 17:08
     */
    @GetMapping("/chat4Transformer")
    public String chat4Transformer(@RequestParam("message") String message) {
//        String answer = assistant4Transformer.chat(message);
//        String sourceFileName = resolveSourceFileName4Transformer(message);
//        return normalizeSource(answer, sourceFileName);

        // contentRetriever4Transformer 已经将文件名放到每段 TextSegment 中，会返回完整的文件名，所以这里不需要再处理文件名
        return assistant4Transformer.chat(message);
    }

//    private String resolveSourceFileName4Transformer(String message) {
//        List<Content> contentList = contentRetriever4Transformer.retrieve(new Query(message));
//        log.info("resolveSourceFileName4Transformer contentList: {}", contentList);
//        return contentList.stream()
//                .map(content -> content.textSegment().metadata().getString(Document.FILE_NAME))
//                .filter(StrUtil::isNotBlank)
//                .findFirst()
//                .orElse(null);
//    }
    
    /**
     * 嵌入存储内容检索器（Embedding Store Content Retriever）
     * @param message
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/8/26 17:07
     */
    @GetMapping("/chat4Embedding")
    public String chat4Embedding(@RequestParam("message") String message) {
        // contentRetriever4Embedding 已经将文件名放到每段 TextSegment 中，会返回完整的文件名，所以这里不需要再处理文件名
        return assistant4Embedding.chat(message);
    }
}
