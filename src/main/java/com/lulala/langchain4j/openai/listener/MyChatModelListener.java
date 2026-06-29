package com.lulala.langchain4j.openai.listener;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 可观测性（Observability）说明参见：https://langchain4j.cn/tutorials/observability.html
 * @author shenjh
 * @version 1.0
 * @since 2026/6/9 10:23
 */
@Slf4j
public class MyChatModelListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("onRequest(): {}", requestContext.chatRequest());

        ChatRequest chatRequest = requestContext.chatRequest();

        List<ChatMessage> messages = chatRequest.messages();
        System.out.println("chatRequest.messages=" + messages);

        ChatRequestParameters parameters = chatRequest.parameters();
        System.out.println("parameters.modelName=" + parameters.modelName());
        System.out.println("parameters.temperature=" + parameters.temperature());
        System.out.println("parameters.topP=" + parameters.topP());
        System.out.println("parameters.topK=" + parameters.topK());
        System.out.println("parameters.frequencyPenalty=" + parameters.frequencyPenalty());
        System.out.println("parameters.presencePenalty=" + parameters.presencePenalty());
        System.out.println("parameters.maxOutputTokens=" + parameters.maxOutputTokens());
        System.out.println("parameters.stopSequences=" + parameters.stopSequences());
        System.out.println("parameters.toolSpecifications=" + parameters.toolSpecifications());
        System.out.println("parameters.toolChoice=" + parameters.toolChoice());
        System.out.println("parameters.responseFormat=" + parameters.responseFormat());

        if (parameters instanceof OpenAiChatRequestParameters openAiParameters) {
            System.out.println("openAiParameters.maxCompletionTokens=" + openAiParameters.maxCompletionTokens());
            System.out.println("openAiParameters.logitBias=" + openAiParameters.logitBias());
            System.out.println("openAiParameters.parallelToolCalls=" + openAiParameters.parallelToolCalls());
            System.out.println("openAiParameters.seed=" + openAiParameters.seed());
            System.out.println("openAiParameters.user=" + openAiParameters.user());
            System.out.println("openAiParameters.store=" + openAiParameters.store());
            System.out.println("openAiParameters.metadata=" + openAiParameters.metadata());
            System.out.println("openAiParameters.serviceTier=" + openAiParameters.serviceTier());
            System.out.println("openAiParameters.reasoningEffort=" + openAiParameters.reasoningEffort());
        }

        System.out.println("requestContext.modelProvider=" + requestContext.modelProvider());

        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put("my-attribute", "my-value");
        System.out.println("my-attribute=" + attributes.get("my-attribute"));
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("onResponse(): {}", responseContext.chatResponse());

        ChatResponse chatResponse = responseContext.chatResponse();

        AiMessage aiMessage = chatResponse.aiMessage();
        System.out.println("chatResponse.aiMessage=" + aiMessage);

        ChatResponseMetadata metadata = chatResponse.metadata();
        System.out.println("metadata.id=" + metadata.id());
        System.out.println("metadata.modelName=" + metadata.modelName());
        System.out.println("metadata.finishReason=" + metadata.finishReason());

        if (metadata instanceof OpenAiChatResponseMetadata openAiMetadata) {
            System.out.println("openAiMetadata.created=" + openAiMetadata.created());
            System.out.println("openAiMetadata.serviceTier=" + openAiMetadata.serviceTier());
            System.out.println("openAiMetadata.systemFingerprint=" + openAiMetadata.systemFingerprint());
        }

        TokenUsage tokenUsage = metadata.tokenUsage();
        System.out.println("tokenUsage.inputTokenCount=" + tokenUsage.inputTokenCount());
        System.out.println("tokenUsage.outputTokenCount=" + tokenUsage.outputTokenCount());
        System.out.println("tokenUsage.totalTokenCount=" + tokenUsage.totalTokenCount());
        if (tokenUsage instanceof OpenAiTokenUsage openAiTokenUsage) {
            // 缓存命中的输入 Token 数
            OpenAiTokenUsage.InputTokensDetails inputTokensDetails = openAiTokenUsage.inputTokensDetails();
            if (inputTokensDetails != null) {
                System.out.println("openAiTokenUsage.inputTokensDetails.cachedTokens=" + inputTokensDetails.cachedTokens());
            }
            // 推理消耗的 Token 数
            OpenAiTokenUsage.OutputTokensDetails outputTokensDetails = openAiTokenUsage.outputTokensDetails();
            if (outputTokensDetails != null) {
                System.out.println("openAiTokenUsage.outputTokensDetails.reasoningTokens=" + outputTokensDetails.reasoningTokens());
            }
        }

        ChatRequest chatRequest = responseContext.chatRequest();
        System.out.println("responseContext.chatRequest=" + chatRequest);

        System.out.println("responseContext.modelProvider=" + responseContext.modelProvider());

        Map<Object, Object> attributes = responseContext.attributes();
        System.out.println("my-attribute=" + attributes.get("my-attribute"));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.info("onError(): {}", errorContext.error().getMessage());

        Throwable error = errorContext.error();
        log.error("onError()", error);

        ChatRequest chatRequest = errorContext.chatRequest();
        System.out.println("errorContext.chatRequest=" + chatRequest);

        System.out.println("errorContext.modelProvider=" + errorContext.modelProvider());

        Map<Object, Object> attributes = errorContext.attributes();
        System.out.println("my-attribute=" + attributes.get("my-attribute"));
    }
}
