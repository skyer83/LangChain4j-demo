package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 请参考 {@link Naive_RAG_Example} 以了解基本背景。
 * <p>
 * LangChain4j 中的高级 RAG 在此描述：https://github.com/langchain4j/langchain4j/pull/538
 * <p>
 * 本示例演示了如何使用 SQL 数据库内容检索器。
 * <p>
 * 警告！虽然有趣且令人兴奋，但 {@link SqlDatabaseContentRetriever} 使用它存在风险！
 * 切勿在生产环境中使用它！数据库用户必须仅具有非常有限的只读权限！
 * 虽然生成的 SQL 会经过一定程度的验证（以确保 SQL 是 SELECT 语句），
 * 但不能保证它完全无害。使用后果自负！
 * <p>
 * 在本示例中，我们将使用一个包含 3 张表（customers、products 和 orders）的内存 H2 数据库。
 * 有关更多详细信息，请参阅 "resources/sql" 目录。
 * <p>
 * 本示例需要 "langchain4j-experimental-sql" 依赖。。
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/advancedRagSqlDatabaseRetrieverExample")
public class _10_Advanced_RAG_SQL_Database_Retreiver_Example {

    @Autowired
    private ChatModel deepseekChatModel;

    private RagExampleAssistant ragExampleAssisant;

    @RequestMapping("/chat")
    public String chat(@RequestParam String query) {
        if (ragExampleAssisant == null) {
            synchronized (this) {
                if (ragExampleAssisant == null) {
                    ragExampleAssisant = createAssistant();
                }
            }
        }
        // 你可以提出诸如‘我们有多少客户？’或‘我们的最畅销产品是什么？’之类的问题。
        // 我们有多少客户？
        /*
            你们有 **5** 位客户。
         */
        // 我们的最畅销产品是什么？
        /*
            你们最畅销的产品是 **Pen**。
         */
        return ragExampleAssisant.answer(query);
    }

    private RagExampleAssistant createAssistant() {
        DataSource dataSource = createDataSource();

        // 虽然生成的 SQL 会经过一定程度的验证（以确保 SQL 是 SELECT 语句），
        // 但不能保证它完全无害。使用后果自负！
        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatModel(deepseekChatModel)
                .maxRetries(2)
                .build();

        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
                .build();
    }

    private static DataSource createDataSource() {

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        String createTablesScript = read("rag-examples/sql/create_tables.sql");
        execute(createTablesScript, dataSource);

        String prefillTablesScript = read("rag-examples/sql/prefill_tables.sql");
        execute(prefillTablesScript, dataSource);

        return dataSource;
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(RagUtils.toPath(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execute(String sql, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sqlStatement : sql.split(";")) {
                statement.execute(sqlStatement.trim());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
