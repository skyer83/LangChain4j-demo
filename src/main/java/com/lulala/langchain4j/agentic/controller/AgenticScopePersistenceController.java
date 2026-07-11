package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.enums.RequestCategory;
import com.lulala.langchain4j.agentic.service.CategoryRouter;
import com.lulala.langchain4j.agentic.service.ExpertRouterAgentWithScopeAccess;
import com.lulala.langchain4j.agentic.persistence.FileAgenticScopeStore;
import com.lulala.langchain4j.agentic.service.LegalExpertWithMemory;
import com.lulala.langchain4j.agentic.service.MedicalExpertWithMemory;
import com.lulala.langchain4j.agentic.service.TechnicalExpertWithMemory;
import com.lulala.langchain4j.agentic.service.UnknownExpert;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * AgenticScope 注册与持久化
 * @author shenjh
 * @version 1.0
 * @since 2026/7/6 20:30
 */
@RestController
@RequestMapping("/agenticScopePersistence")
public class AgenticScopePersistenceController {

    @Autowired
    private ChatModel gptChatModel;

    private FileAgenticScopeStore scopeStore;
    private ExpertRouterAgentWithScopeAccess expertRouterAgent;

    @PostConstruct
    public void init() {
        scopeStore = new FileAgenticScopeStore(Path.of("target", "agentic-scope-store"));
        AgenticScopePersister.setStore(scopeStore);
        expertRouterAgent = buildExpertRouterAgent();
    }

    /**
     * 执行带 MemoryId 的 agentic system，执行结束后 AgenticScope 会保留在内存注册表并写入文件。
     * 示例：/agenticScopePersistence/ask?memoryId=demo-1&request=我的腿摔断了，我该怎么办？
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(
            @RequestParam(name = "memoryId", defaultValue = "demo-1") String memoryId,
            @RequestParam(name = "request", defaultValue = "我的腿摔断了，我该怎么办？") String request
    ) {
        String response = expertRouterAgent.ask(memoryId, request);

        Map<String, Object> result = baseInfo(memoryId);
        result.put("request", request);
        result.put("response", response);
        result.put("scope", scopeInfo(memoryId));
        return result;
    }

    /**
     * 从根代理的 AgenticScopeAccess 读取内存注册表中的 AgenticScope。
     */
    @GetMapping("/scope")
    public Map<String, Object> scope(@RequestParam(name = "memoryId", defaultValue = "demo-1") String memoryId) {
        Map<String, Object> result = baseInfo(memoryId);
        result.put("scope", scopeInfo(memoryId));
        return result;
    }

    /**
     * 从内存注册表驱逐指定 memoryId 的 AgenticScope。
     */
    @GetMapping("/evict")
    public Map<String, Object> evict(@RequestParam(name = "memoryId", defaultValue = "demo-1") String memoryId) {
        boolean evicted = expertRouterAgent.evictAgenticScope(memoryId);

        Map<String, Object> result = baseInfo(memoryId);
        result.put("evicted", evicted);
        result.put("scopeAfterEvict", scopeInfo(memoryId));
        return result;
    }

    /**
     * 查看当前文件持久化 Store 中保存的 AgenticScope。
     */
    @GetMapping("/store")
    public Map<String, Object> store() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeDirectory", scopeStore.directory().toAbsolutePath().toString());
        result.put("storedKeys", storedKeys());
        result.put("files", scopeStore.fileNames());
        return result;
    }

    /**
     * 清理文件持久化 Store，便于重复演示。
     */
    @GetMapping("/clearStore")
    public Map<String, Object> clearStore() {
        scopeStore.clear();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeDirectory", scopeStore.directory().toAbsolutePath().toString());
        result.put("storedKeys", storedKeys());
        result.put("files", scopeStore.fileNames());
        return result;
    }

    private ExpertRouterAgentWithScopeAccess buildExpertRouterAgent() {
        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(gptChatModel)
                .outputKey("category")
                .build();

        MedicalExpertWithMemory medicalExpert = AgenticServices
                .agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        LegalExpertWithMemory legalExpert = AgenticServices
                .agentBuilder(LegalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .summarizedContext("medical", "technical")
                .outputKey("response")
                .build();

        TechnicalExpertWithMemory technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpertWithMemory.class)
                .chatModel(gptChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        UnknownExpert unknownExpert = AgenticServices
                .agentBuilder(UnknownExpert.class)
                .chatModel(gptChatModel)
                .outputKey("response")
                .build();

        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.UNKNOWN, unknownExpert)
                .build();

        return AgenticServices
                .sequenceBuilder(ExpertRouterAgentWithScopeAccess.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();
    }

    private Map<String, Object> baseInfo(String memoryId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memoryId", memoryId);
        result.put("storeDirectory", scopeStore.directory().toAbsolutePath().toString());
        result.put("storedKeys", storedKeys());
        return result;
    }

    private Map<String, Object> scopeInfo(String memoryId) {
        AgenticScope scope = expertRouterAgent.getAgenticScope(memoryId);
        if (scope == null) {
            return Map.of("existsInRegistry", false);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("existsInRegistry", true);
        result.put("memoryId", scope.memoryId());
        result.put("state", scope.state());
        result.put("context", scope.contextAsConversation());
        result.put("agentInvocations", scope.agentInvocations().size());
        return result;
    }

    private List<Map<String, Object>> storedKeys() {
        Set<AgenticScopeKey> keys = scopeStore.getAllKeys();
        return keys.stream()
                .map(key -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("agentId", key.agentId());
                    item.put("memoryId", key.memoryId());
                    return item;
                })
                .toList();
    }
}
