package com.lulala.langchain4j.rag.examples.controller;

import com.lulala.langchain4j.rag.examples.service.RagExampleAssistant;
import com.lulala.langchain4j.rag.utils.RagUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 本示例演示了如何实现一个基础版检索增强生成（RAG）应用。
 * 所谓“基础版”，是指我们不采用任何高级 RAG 技术。
 * 在每次与大语言模型（LLM）交互时，我们将：
 * 1. 直接获取用户的原始查询。
 * 2. 使用嵌入模型将其转换为向量表示。
 * 3. 利用该查询向量在嵌入存储（其中包含文档的若干小片段）中检索最相关的 X 个片段。
 * 4. 将检索到的片段附加到用户查询之后。
 * 5. 将组合后的输入（用户查询 + 检索片段）发送给大语言模型。
 * 6. 并期望：
 *   - 用户的查询表述清晰，且包含了检索所需的全部关键信息；
 *   - 检索到的片段与用户查询高度相关。
 * @author shenjh
 * @version 1.0
 * @since 2026/8/28 13:41
 */
@Slf4j
@RestController
@RequestMapping("/rag/naiveRagExample")
public class Naive_RAG_Example {

    @Autowired
    private EmbeddingModel embeddingModelOfZhV15;
    @Autowired
    private ChatModel deepseekChatModel;

    @RequestMapping("/chat")
    public String chat(@RequestParam String query) {
        String relativePath = "rag-examples/documents/miles-of-smiles-terms-of-use.txt";
        RagExampleAssistant ragExampleAssisant = createAssistant(relativePath);
        // 我们可以提出如下问题：
        // - 我可以取消预订吗？
        // - 我出了事故，需要额外付费吗？
        return ragExampleAssisant.answer(query);
    }

    private RagExampleAssistant createAssistant(String documentPath) {
        // 现在，让我们加载一个用于 RAG 的文档。
        // 我们将使用一家虚构的租车公司“Miles of Smiles”的使用条款。
        // 在本示例中，我们仅导入单个文档，但您可以根据需要加载任意数量的文档。
        // LangChain4j 内置了从多种来源加载文档的支持：
        // 文件系统、URL、Amazon S3、Azure Blob Storage、GitHub、腾讯云 COS。
        // 此外，LangChain4j 还支持解析多种文档类型：
        // text、pdf、doc、xls、ppt。
        // 当然，您也可以手动从其他来源导入数据。
        DocumentParser documentParser = new TextDocumentParser();
        Document document = FileSystemDocumentLoader.loadDocument(RagUtils.toPath(documentPath), documentParser);

        // 现在，我们需要将该文档拆分为更小的片段，也称为“文本块”。
        // 这种做法使我们能够针对用户的查询，仅将相关的片段发送给大语言模型，
        // 而无需发送整个文档。例如，如果用户询问取消政策，
        // 我们只需识别并发送与取消相关的片段即可。
        // 一个不错的起点是使用递归文档分割器，它首先尝试
        // 按段落进行拆分。如果某个段落过大，无法放入单个片段中，
        // 分割器便会递归地依次按换行符、句子，最后在必要时按单词进行拆分，
        // 以确保每段文本都能容纳进单个片段之中。
        //
        // maxOverlapSizeInChars 参数设置大于 0 。
        // 当文本按“自然边界”切分时（重叠可能不生效）：
        //  如果 DocumentSplitter 在切分时，刚好在指定的 maxSegmentSizeInChars 附近找到了一个完美的自然分隔符（例如一个完整的句号 。、换行符 \n 或空格），
        //  切分器会优先在这个自然边界处断开。此时，由于文本本身已经是一个完整的句子或段落，即便有重叠设置，系统也可能认为没有必要强行制造重叠，或者重叠部分刚好是标点符号。
        // 当文本被“强制截断”时（重叠必然生效）：
        //  如果文本非常长，且在 maxSegmentSizeInChars 的范围内没有找到合适的自然分隔符（例如遇到了一段没有换行、没有标点、甚至没有空格的超长连续字符串），
        //  切分器为了保证片段大小不超标，会进行强制硬截断（Hard Split）。在这种由于找不到自然边界而被迫“一刀切”的情况下，maxOverlapSizeInChars 就会发挥关键作用，
        //  强制将截断点之前的 N 个字符复制到下一个片段的开头，以弥补硬截断带来的语义丢失。
        // 重叠区的大小通常建议设置为文本块大小（maxSegmentSizeInChars）的 10% 到 20% 左右。
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 50);
        List<TextSegment> segments = documentSplitter.split(document);

        // 现在，我们需要对这些片段进行嵌入（也称为“向量化”）。
        // 嵌入是执行相似度搜索所必需的。
        // 在本示例中，我们将使用本地进程内嵌入模型，但您可以选择任何受支持的模型。
        // LangChain4j 目前支持超过 10 种主流的嵌入模型提供商。
        List<Embedding> embeddings = embeddingModelOfZhV15.embedAll(segments).content();

        // 接下来，我们将把这些嵌入向量存储到嵌入存储（也称为“向量数据库”）中。
        // 该存储库将用于在每次与大语言模型（LLM）交互时检索相关的文本片段。
        // 为了简单起见，本示例使用的是内存中的嵌入存储，但您可以选择任何受支持的存储库。
        // LangChain4j 目前支持超过 15 种主流的嵌入存储。
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 我们也可以使用 EmbeddingStoreIngestor（嵌入存储摄取器），通过更简单的 API 来封装上述手动步骤。
        // 有关使用 EmbeddingStoreIngestor 的示例，请参阅 _01_Advanced_RAG_with_Query_Compression_Example。

        // 内容检索器（Content Retriever）负责根据用户查询检索相关内容。
        // 目前，它支持检索文本片段，但未来的增强功能将包括对
        // 图像、音频等其他模态的支持。
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModelOfZhV15)
                .maxResults(2)
                .minScore(0.5)
                .build();

        // （可选）我们可以使用聊天记忆（Chat Memory），从而能够与大语言模型（LLM）进行多轮对话，
        // 并使其能够记住之前的交互内容。
        // 目前，LangChain4j 提供了两种聊天记忆的实现方式：
        // MessageWindowChatMemory（基于消息窗口的聊天记忆）和 TokenWindowChatMemory（基于 Token 窗口的聊天记忆）。
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 最后一步是构建我们的 AI 服务，
        // 并对其进行配置，以使用我们在上面创建的各个组件。
        return AiServices.builder(RagExampleAssistant.class)
                .chatModel(deepseekChatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }
}
