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
        log.info("chatRequest.messages={}", messages);

        ChatRequestParameters parameters = chatRequest.parameters();
        log.info("parameters.modelName={}", parameters.modelName());
        log.info("parameters.temperature={}", parameters.temperature());
        log.info("parameters.topP={}", parameters.topP());
        log.info("parameters.topK={}", parameters.topK());
        log.info("parameters.frequencyPenalty={}", parameters.frequencyPenalty());
        log.info("parameters.presencePenalty={}", parameters.presencePenalty());
        log.info("parameters.maxOutputTokens={}", parameters.maxOutputTokens());
        log.info("parameters.stopSequences={}", parameters.stopSequences());
        log.info("parameters.toolSpecifications={}", parameters.toolSpecifications());
        log.info("parameters.toolChoice={}", parameters.toolChoice());
        log.info("parameters.responseFormat={}", parameters.responseFormat());

        if (parameters instanceof OpenAiChatRequestParameters openAiParameters) {
            log.info("openAiParameters.maxCompletionTokens={}", openAiParameters.maxCompletionTokens());
            log.info("openAiParameters.logitBias={}", openAiParameters.logitBias());
            log.info("openAiParameters.parallelToolCalls={}", openAiParameters.parallelToolCalls());
            log.info("openAiParameters.seed={}", openAiParameters.seed());
            log.info("openAiParameters.user={}", openAiParameters.user());
            log.info("openAiParameters.store={}", openAiParameters.store());
            log.info("openAiParameters.metadata={}", openAiParameters.metadata());
            log.info("openAiParameters.serviceTier={}", openAiParameters.serviceTier());
            log.info("openAiParameters.reasoningEffort={}", openAiParameters.reasoningEffort());
        }

        log.info("requestContext.modelProvider={}", requestContext.modelProvider());

        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put("my-attribute", "my-value");
        log.info("onRequest my-attribute={}", attributes.get("my-attribute"));
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("onResponse(): {}", responseContext.chatResponse());

        ChatResponse chatResponse = responseContext.chatResponse();

        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("chatResponse.aiMessage={}", aiMessage);

        ChatResponseMetadata metadata = chatResponse.metadata();
        log.info("metadata.id={}", metadata.id());
        log.info("metadata.modelName={}", metadata.modelName());
        log.info("metadata.finishReason={}", metadata.finishReason());

        if (metadata instanceof OpenAiChatResponseMetadata openAiMetadata) {
            log.info("openAiMetadata.created={}", openAiMetadata.created());
            log.info("openAiMetadata.serviceTier={}", openAiMetadata.serviceTier());
            log.info("openAiMetadata.systemFingerprint={}", openAiMetadata.systemFingerprint());
        }

        TokenUsage tokenUsage = metadata.tokenUsage();
        log.info("tokenUsage.inputTokenCount={}", tokenUsage.inputTokenCount());
        log.info("tokenUsage.outputTokenCount={}", tokenUsage.outputTokenCount());
        log.info("tokenUsage.totalTokenCount={}", tokenUsage.totalTokenCount());
        if (tokenUsage instanceof OpenAiTokenUsage openAiTokenUsage) {
            // 缓存命中的输入 Token 数
            OpenAiTokenUsage.InputTokensDetails inputTokensDetails = openAiTokenUsage.inputTokensDetails();
            if (inputTokensDetails != null) {
                log.info("openAiTokenUsage.inputTokensDetails.cachedTokens={}", inputTokensDetails.cachedTokens());
            }
            // 推理消耗的 Token 数
            OpenAiTokenUsage.OutputTokensDetails outputTokensDetails = openAiTokenUsage.outputTokensDetails();
            if (outputTokensDetails != null) {
                log.info("openAiTokenUsage.outputTokensDetails.reasoningTokens={}", outputTokensDetails.reasoningTokens());
            }
        }

        ChatRequest chatRequest = responseContext.chatRequest();
        log.info("responseContext.chatRequest={}", chatRequest);

        log.info("responseContext.modelProvider={}", responseContext.modelProvider());

        Map<Object, Object> attributes = responseContext.attributes();
        log.info("onResponse my-attribute={}", attributes.get("my-attribute"));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.info("onError(): {}", errorContext.error().getMessage());

        Throwable error = errorContext.error();
        log.error("onError()", error);

        ChatRequest chatRequest = errorContext.chatRequest();
        log.info("errorContext.chatRequest={}", chatRequest);

        log.info("errorContext.modelProvider={}", errorContext.modelProvider());

        Map<Object, Object> attributes = errorContext.attributes();
        log.info("onError my-attribute={}", attributes.get("my-attribute"));
    }
}
