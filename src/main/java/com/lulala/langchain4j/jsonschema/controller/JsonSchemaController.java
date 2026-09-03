package com.lulala.langchain4j.jsonschema.controller;

import com.lulala.langchain4j.jsonschema.service.PersonExtractor1;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 参见：
 * <pre>
 *     https://langchain4j.cn/tutorials/structured-outputs.html#%E5%9C%A8-chatmodel-%E4%B8%AD%E4%BD%BF%E7%94%A8-json-schema
 *     https://github.com/langchain4j/langchain4j/blob/main/langchain4j/src/test/java/dev/langchain4j/service/AiServicesWithJsonSchemaWithDescriptionsIT.java
 *     https://github.com/langchain4j/langchain4j/blob/main/langchain4j/src/test/java/dev/langchain4j/service/AiServicesWithJsonSchemaWithRequiredIT.java
 * </pre>
 * @author shenjh
 * @since 2026/9/2 11:17
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/jsonSchema")
public class JsonSchemaController {

    @Autowired
    private ChatModel deepseekChatModelOfJson;
    @Autowired
    private ChatModel gptChatModelOfJson;

    @RequestMapping("/extractPersonWithPrimitivesOfPojo")
    public String extractPersonWithPrimitivesOfPojo() {
        /*
            UserMessage { name = null, contents = [TextContent { text = "张三37岁，身高1.78米，单身。
            You must answer strictly in the following JSON format: {
            "name": (姓名; type: string),
            "age": (年龄; type: integer),
            "height": (身高; type: double),
            "married": (是否已婚; type: boolean)
            }" }]
         */
        PersonExtractor1 personExtractor1 = AiServices.create(PersonExtractor1.class, deepseekChatModelOfJson);
        String text = "张三37岁，身高1.78米，单身。";
        /*
            aiMessage = AiMessage { text = "{
            "name": "张三",
            "age": 37,
            "height": 1.78,
            "married": false
            }",
         */
        return personExtractor1.extractPersonFrom(text).toString();
    }

    @RequestMapping("/extractPersonWithPrimitivesOfJson4OpenAi")
    public String extractPersonWithPrimitivesOfJson4OpenAi() {
        HashMap<String, JsonSchemaElement> jsonObjectSchemaHashMap = new LinkedHashMap<>() {{
            put("name", JsonStringSchema.builder().description("姓名").build());
            put("age", JsonIntegerSchema.builder().description("年龄").build());
            put("height", JsonNumberSchema.builder().description("身高").build());
            put("married", JsonBooleanSchema.builder().description("是否已婚").build());
        }};
        JsonObjectSchema rootElement = JsonObjectSchema.builder()
                .description("一个人")
                .addProperties(jsonObjectSchemaHashMap)
                .build();
        JsonSchema jsonSchema = JsonSchema.builder()
                // OpenAI 的 structured output 要求 schema name 只能包含字母、数字、下划线或连字符
                .name("person")
                .rootElement(rootElement)
                .build();

        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();

        String text = "张三37岁，身高1.78米，单身。";
        UserMessage userMessage = UserMessage.userMessage(text);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .responseFormat(responseFormat)
                .build();
        // DeepSeek 当前支持 json_object，不支持 OpenAI 的 json_schema response_format。
        // 这里需使用 GPT 的 json_schema response_format。
        ChatResponse chatResponse = gptChatModelOfJson.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();
        return aiMessage.text();
    }

    @RequestMapping("/extractPersonWithPrimitivesOfJson4Deepseek")
    public String extractPersonWithPrimitivesOfJson4Deepseek() {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .build();

        String text = "张三37岁，身高1.78米，单身。";
        String prompt = """
                请从下面的文本中提取人物信息。
                只返回合法 JSON，不要返回 Markdown 代码块、解释或其他内容。
                JSON 必须包含以下字段：
                {
                  "name": "姓名",
                  "age": 0,
                  "height": 0.0,
                  "married": false
                }
                文本：%s
                """.formatted(text);
        UserMessage userMessage = UserMessage.userMessage(prompt);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .responseFormat(responseFormat)
                .build();
        /*
            chatRequest.messages=[UserMessage { name = null, contents = [TextContent { text = "请从下面的文本中提取人物信息。
            只返回合法 JSON，不要返回 Markdown 代码块、解释或其他内容。
            JSON 必须包含以下字段：
            {
              "name": "姓名",
              "age": 0,
              "height": 0.0,
              "married": false
            }
            文本：张三37岁，身高1.78米，单身。
            " }], attributes = {} }]
         */
        ChatResponse chatResponse = deepseekChatModelOfJson.chat(chatRequest);
        /*
            aiMessage = AiMessage { text = "{
              "name": "张三",
              "age": 37,
              "height": 1.78,
              "married": false
            }"
         */
        AiMessage aiMessage = chatResponse.aiMessage();
        return aiMessage.text();
    }
}
