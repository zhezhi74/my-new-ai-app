package com.example.chatservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AiExtractionService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 假设你使用的是 deepseek 或者自定义模型
    @Value("${ai.chat.model:deepseek-chat}")
    private String chatModel;

    public AiExtractionService(@Qualifier("deepSeekChatService") OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    // 核心 Prompt（之前探讨过的企业级指令）
    private static final String SYSTEM_PROMPT =
            "你是一个专业的临床医学信息萃取引擎。\n" +
                    "请阅读以下患者与AI的对话记录，滤除无意义闲聊，严格按以下 JSON 格式输出结构化摘要：\n" +
                    "{\n" +
                    "  \"desc\": \"以医生口吻总结主诉和现病史，不超过150字。\",\n" +
                    "  \"tags\": [\"提取3-5个核心症状短标签\", \"如：心悸\", \"胸闷\"]\n" +
                    "}\n" +
                    "注意：只输出合法的 JSON，不要任何 Markdown 标记（如 ```json）。";

    /**
     * 将原始聊天记录转化为结构化数据
     */
    public ExtractedData extractFromChatHistory(String rawChatHistory) {
        log.info("开始进行 AI 医疗信息萃取，文本长度: {}", rawChatHistory.length());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.add(new ChatMessage("user", "【原始对话记录】：\n" + rawChatHistory));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModel)
                .messages(messages)
                .temperature(0.1) // 保持极低的温度，确保 JSON 格式稳定和医学严谨性
                .build();

        try {
            // 1. 调用大模型
            String responseText = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            // 清理可能带有的 markdown 标记
            responseText = responseText.replace("```json", "").replace("```", "").trim();
            log.info("AI 萃取结果: {}", responseText);

            // 2. 解析 JSON
            JsonNode jsonNode = objectMapper.readTree(responseText);
            ExtractedData data = new ExtractedData();
            data.setDesc(jsonNode.has("desc") ? jsonNode.get("desc").asText() : "未能成功萃取主诉");

            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags")) {
                for (JsonNode tagNode : jsonNode.get("tags")) {
                    tags.add(tagNode.asText());
                }
            }
            data.setTags(tags);
            return data;

        } catch (Exception e) {
            log.error("AI 萃取失败", e);
            ExtractedData fallback = new ExtractedData();
            fallback.setDesc("AI 萃取引擎暂时离线，或解析 JSON 失败。");
            fallback.setTags(Arrays.asList("系统错误", "需人工审核"));
            return fallback;
        }
    }

    // 内部类用于承载解析结果
    @lombok.Data
    public static class ExtractedData {
        private String desc;
        private List<String> tags;
    }
}