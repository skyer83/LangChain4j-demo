package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.PersonalizedAssistant;
import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本上下文。
 * <p>
 * 有关元数据过滤的更多信息，请访问：https://github.com/langchain4j/langchain4j/pull/610
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 15:07
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagWithMetadataFilteringExample")
public class _05_Advanced_RAG_with_Metadata_Filtering_Examples {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/staticMetadataFilterExample")
    public String staticMetadataFilterExample() {
        TextSegment dogsSegment = TextSegment.from("关于狗的文章 ...", Metadata.metadata("animal", "狗"));
        TextSegment birdsSegment = TextSegment.from("关于鸟的文章 ...", Metadata.metadata("animal", "鸟"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModelOfZhV15.embed(dogsSegment).content(), dogsSegment);
        embeddingStore.add(embeddingModelOfZhV15.embed(birdsSegment).content(), birdsSegment);

        Filter onlyDogs = MetadataFilterBuilder.metadataKey("animal").isEqualTo("狗");

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                // 通过指定静态过滤器，我们可以将搜索范围限制为仅包含关于狗（dogs）的文本片段。
                .filter(onlyDogs)
                .build();

        RagExampleAssistant ragExampleAssistant = AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .contentRetriever(contentRetriever)
                .build();
        return ragExampleAssistant.answer("那种动物?");
    }

    @RequestMapping("/dynamicMetadataFilterExample")
    public String dynamicMetadataFilterExample() {
        TextSegment user1Segment = TextSegment.from("我最喜欢的颜色是绿色", Metadata.metadata("userId", "1"));
        TextSegment user2Segment = TextSegment.from("我最喜欢的颜色是红色", Metadata.metadata("userId", "2"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModelOfZhV15.embed(user1Segment).content(), user1Segment);
        embeddingStore.add(embeddingModelOfZhV15.embed(user2Segment).content(), user2Segment);

        Function<Query, Filter> filterByUserId = (query) -> {
            MetadataFilterBuilder filterBuilder = MetadataFilterBuilder.metadataKey("userId");
            return filterBuilder.isEqualTo(query.metadata().chatMemoryId().toString());
        };

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                // 通过指定动态过滤器，我们可以将搜索范围限制为仅属于当前用户的文本片段。
                .dynamicFilter(filterByUserId)
                .build();

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.withMaxMessages(10);

        PersonalizedAssistant personalizedAssistant = AiServices.builder(PersonalizedAssistant.class)
                .chatModel(deepseekChatModel)
                .contentRetriever(contentRetriever)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
        String answer1 = personalizedAssistant.chat("1", "裙子穿什么颜色最好？");
        String answer2 = personalizedAssistant.chat("2", "裙子穿什么颜色最好？");
        return "User 1 answer: " + answer1 + "\n" + "User 2 answer: " + answer2;
    }
}
