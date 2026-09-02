package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 本示例演示了如何使用 LangChain4j 的底层 API 来实现 RAG。
 * 请查看其他包以了解使用高层 API（AI 服务）的示例。
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/lowLevelNaiveRagExample")
public class _01_Low_Level_Naive_RAG_Example {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/chat")
    public String chat() {
        // 加载包含您想与模型进行‘聊天’的信息的文档。
        String documentPath = "rag-examples/example-files/story-about-happy-carrot.txt";
        Document document = RagUtils.loadDocument(documentPath);

        // 将文档拆分为每段 300 个 token（词元）的片段
        // 重叠部分的最大尺寸（以 token 为单位）为 50 个 token
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 50,
                new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI));
        List<TextSegment> segments = splitter.split(document);

        // 使用嵌入模型（Embedding Model）对文本片段进行嵌入处理（将它们转换为能够代表其语义的向量）。
        // 将切分好的文本块（Segments/Chunks）通过模型转化为多维数组/向量（Vectors），以便后续存入向量数据库中进行语义检索。
        Response<List<Embedding>> embedResponse = embeddingModelOfZhV15.embedAll(segments);
        List<Embedding> embeddings = embedResponse.content();

        // 将嵌入向量（Embeddings）存储到向量存储（Embedding Store）中，以便进行后续的搜索与检索。
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 指定您想要向模型提问的问题。
        String question = "查理是谁？";

        // 对问题进行嵌入处理。
        Response<Embedding> questionEmbed = embeddingModelOfZhV15.embed(question);
        Embedding questionEmbedding = questionEmbed.content();

        // 通过语义相似度在向量存储（Embedding Store）中查找相关的嵌入向量
        // 您可以尝试调整下面的参数，为您的特定应用场景找到最佳配置（或最佳平衡点）
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(3)
                .minScore(0.7)
                .build();
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.search(embeddingSearchRequest).matches();

        // 将检索到的文本片段拼接成一个字符串，用两个换行符分隔
        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));

        // 为模型创建一个提示词（Prompt），其中需包含用户的问题和检索到的相关文本片段。
//        PromptTemplate promptTemplate = PromptTemplate.from(
//                "Answer the following question to the best of your ability:\n"
//                        + "\n"
//                        + "Question:\n"
//                        + "{{question}}\n"
//                        + "\n"
//                        + "Base your answer on the following information:\n"
//                        + "{{information}}");
        PromptTemplate promptTemplate = PromptTemplate.from(
                "请尽你所能回答以下问题：\n"
                        + "\n"
                        + "问题：\n"
                        + "{{question}}\n"
                        + "\n"
                        + "请基于以下信息回答：\n"
                        + "{{information}}");

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("information", information);
        Prompt prompt = promptTemplate.apply(variables);

        // 将提示词（Prompt）发送给如： OpenAI/Deepseek 聊天模型，并获取回复。
        ChatResponse chatResponse = deepseekChatModel.chat(prompt.toUserMessage());
        AiMessage aiMessage = chatResponse.aiMessage();
        return aiMessage.text();
    }
}
