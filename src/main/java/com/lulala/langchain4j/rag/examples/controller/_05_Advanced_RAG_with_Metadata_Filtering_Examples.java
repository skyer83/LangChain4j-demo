package com.lulala.langchain4j.rag.examples.controller;

import cn.hutool.core.util.StrUtil;
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
import dev.langchain4j.store.embedding.filter.builder.sql.LanguageModelSqlFilterBuilder;
import dev.langchain4j.store.embedding.filter.builder.sql.TableDefinition;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** 从 Markdown SQL（不区分大小写） 代码块中提取 SQL 脚本 */
    private static final Pattern SQL_CODE_BLOCK_PATTERN = Pattern.compile("(?is)```\\s*sql\\s*\\R(.*?)\\R?```");

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

    @RequestMapping("/llmGeneratedMetadataFilterExample")
    public String llmGeneratedMetadataFilterExample() {
        TextSegment forrestGump = TextSegment.from("阿甘正传", Metadata.metadata("genre", "剧情片").put("year", 1994));
        TextSegment groundhogDay = TextSegment.from("偷天情缘", Metadata.metadata("genre", "喜剧片").put("year", 1993));
        TextSegment dieHard = TextSegment.from("虎胆龙威", Metadata.metadata("genre", "动作片").put("year", 1998));

        // 请将元数据键（metadata keys）描述得就像它们是 SQL 表中的列一样。
        // 例如，"genre" 是一个元数据键，"剧情片" 是该键的值。
        TableDefinition tableDefinition = TableDefinition.builder()
                .name("movies")
                .addColumn("genre", "VARCHAR", "以下选项之一：[喜剧片, 剧情片, 动作片]")
                .addColumn("year", "INT")
                .build();
        LanguageModelSqlFilterBuilder sqlFilterBuilder = new LanguageModelSqlFilterBuilder(deepseekChatModel, tableDefinition) {
            @Override
            protected String clean(String sql) {
                return cleanGeneratedSql(sql);
            }
        };

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        String forrestGumpEmbeddingId = embeddingStore.add(embeddingModelOfZhV15.embed(forrestGump).content(), forrestGump);
        String groundhogDayEmbeddingId = embeddingStore.add(embeddingModelOfZhV15.embed(groundhogDay).content(), groundhogDay);
        String dieHardEmbeddingId = embeddingStore.add(embeddingModelOfZhV15.embed(dieHard).content(), dieHard);
        // Embedding IDs: 9584d692-4cc5-4da6-a707-c8693ff25cb0 df22e61f-628b-46d5-a7dd-685fd3ae42f7 f12f5321-8e71-4b17-962d-8bcdd9ceecdf
        log.info("Embedding IDs: {} {} {}", forrestGumpEmbeddingId, groundhogDayEmbeddingId, dieHardEmbeddingId);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                // 大语言模型（LLM）将动态生成过滤器。
                .dynamicFilter(sqlFilterBuilder::build)
                .build();

        RagExampleAssistant ragExampleAssistant = AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .contentRetriever(contentRetriever)
                .build();

        // UserMessage
        /*
            [UserMessage { name = null, contents = [TextContent { text = "### Instructions:
            Your task is to convert a question into a SQL query, given a Postgres database schema.
            Adhere to these rules:
            - **Deliberately go through the question and database schema word by word** to appropriately answer the question
            - **Use Table Aliases** to prevent ambiguity. For example, `SELECT table1.col1, table2.col1 FROM table1 JOIN table2 ON table1.id = table2.id`.
            - When creating a ratio, always cast the numerator as float

            ### Input:
            Generate a SQL query that answers the question `给我推荐一部90年代的好剧情片。`.
            This query will run on a database whose schema is represented in this string:
            CREATE TABLE movies (
                genre VARCHAR, -- 以下选项之一：[喜剧片, 剧情片, 动作片]
                year INT,
            );

            ### Response:
            Based on your instructions, here is the SQL query I have generated to answer the question `给我推荐一部90年代的好剧情片。`:
            ```sql" }], attributes = {} }]
         */
        // AiMessage
        /*
            AiMessage { text = "由于表中没有评分/评价字段，无法直接判断“好”，这里把“好剧情片”理解为类型为“剧情片”，并推荐一部 90 年代（1990–1999）的电影：

            ```sql
            SELECT m.genre, m.year
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            LIMIT 1;
            ```", thinking = null, toolExecutionRequests = [], attributes = {} }
         */
        // LanguageModelSqlFilterBuilder 未清洗 SQL 脚本时，“由于表中没有评分/评价字段...的电影：```sql...```”一起参与了 SQL 解析，需将这部分一并清洗掉
        /*
            Failed parsing the following SQL: '由于表中没有评分/评价字段，无法直接判断“最好的”，这里把“好剧情片”理解为类型为“剧情片”，并推荐一部 90 年代（1990–1999）的电影：

            ```sql
            SELECT m.genre, m.year
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            LIMIT 1;
            ```'
            java.lang.RuntimeException: net.sf.jsqlparser.JSQLParserException: net.sf.jsqlparser.parser.TokenMgrException: Lexical error at line 1, column 45.  Encountered: '\uff0c' (65292),
                at dev.langchain4j.store.embedding.filter.parser.sql.SqlFilterParser.parse(SqlFilterParser.java:117)
         */
        // LanguageModelSqlFilterBuilder 清洗 SQL 脚本后
        /*
            SELECT m.genre, m.year
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            LIMIT 1;
         */
        return ragExampleAssistant.answer("给我推荐一部90年代的最好的剧情片。");
    }
    
    /**
     * 清洗 SQL 脚本，解决因大语言模型返回的不是纯SQL脚本，而是如以下内容：
     * <pre>
     * 由于表中没有评分/评价字段，无法直接判断“最好的”，这里把“好剧情片”理解为类型为“剧情片”，并推荐一部 90 年代（1990–1999）的电影：
     *
     *             ```sql
     *             SELECT m.genre, m.year
     *             FROM movies AS m
     *             WHERE m.genre = '剧情片'
     *               AND m.year BETWEEN 1990 AND 1999
     *             LIMIT 1;
     *             ```
     * </pre>
     * 导致解析 SQL 报错问题：
     * <pre>
     *     由于表中没有评分/评价字段，无法直接判断“最好的”，这里把“好剧情片”理解为类型为“剧情片”，并推荐一部 90 年代（1990–1999）的电影：
     *
     *     ```sql
     *     SELECT m.genre, m.year
     *     FROM movies AS m
     *     WHERE m.genre = '剧情片'
     *       AND m.year BETWEEN 1990 AND 1999
     *     LIMIT 1;
     *     ```
     *     java.lang.RuntimeException: net.sf.jsqlparser.JSQLParserException: net.sf.jsqlparser.parser.TokenMgrException: Lexical error at line 1, column 45.  Encountered: '\uff0c' (65292),
     *     at dev.langchain4j.store.embedding.filter.parser.sql.SqlFilterParser.parse(SqlFilterParser.java:117)
     * </pre>
     * @param sql
     * @return java.lang.String 
     * @author shenjh
     * @since 2026/9/2 10:09
     */
    private static String cleanGeneratedSql(String sql) {
        /*
            清洗前的 Original SQL:
            由于表中没有评分/评价字段，无法直接判断“最好的”，这里把“好剧情片”理解为类型为“剧情片”，并推荐一部 90 年代（1990–1999）的电影：

            ```sql
            SELECT m.genre, m.year
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            LIMIT 1;
            ```
         */
        /*
            清洗前的 Original SQL:
            ```sql
            -- 注意：schema 中没有评分/票房等字段，无法严格判断“最好”；这里按年份最新返回一部 90 年代剧情片。
            SELECT m.*
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            ORDER BY m.year DESC
            LIMIT 1;
            ```
         */
        log.info("清洗前的 Original SQL:\n{}", sql);
        if (StrUtil.isBlank(sql)) {
            log.info("清洗前的 Original SQL 为空，不做清洗");
            return sql;
        }
        String cleaned = sql.trim();

        Matcher sqlCodeBlockMatcher = SQL_CODE_BLOCK_PATTERN.matcher(cleaned);
        if (sqlCodeBlockMatcher.find()) {
            cleaned = sqlCodeBlockMatcher.group(1).trim();
            log.info("从 Markdown SQL 代码块中提取 SQL 脚本");
        }

        int selectIndex = cleaned.toUpperCase().indexOf("SELECT");
        if (selectIndex > 0) {
            cleaned = cleaned.substring(selectIndex).trim();
        }
        
        /*
            清洗后的 SQL:
            SELECT m.genre, m.year
            FROM movies AS m
            WHERE m.genre = '剧情片'
              AND m.year BETWEEN 1990 AND 1999
            LIMIT 1;
         */
        log.info("清洗后的 SQL:\n{}", cleaned);

        return cleaned;
    }
}
