package com.example.chatservice.service;

// 【新增】: 导入我们新创建的 Feign 客户端
import com.example.chatservice.client.KnowledgeServiceClient;
import com.example.chatservice.common.Result;
import com.example.chatservice.entity.ChatHistory;
import com.example.chatservice.entity.Conversation;
import com.example.chatservice.repository.ChatHistoryRepository;
import com.example.chatservice.repository.ConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired; // 【新增】
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    private static final int MAX_HISTORY_SIZE = 10;
    private static final String REDIS_HISTORY_KEY_PREFIX = "chat_history:conv:";
    private static final int KNOWLEDGE_SEARCH_TOP_N = 3;

    private final ChatHistoryRepository chatHistoryRepository;
    private final OpenAiService openAiService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    // 【删除】: 不再直接依赖 KnowledgeService
    // private final KnowledgeService knowledgeService;
    // 【新增】: 转而依赖我们创建的 Feign 客户端
    private final KnowledgeServiceClient knowledgeServiceClient;
    private final ConversationRepository conversationRepository;

    @Value("${ai.deepseek.chat-model}")
    private String chatModelName;

    // 【核心修改】: 修改构造函数，注入 KnowledgeServiceClient 而不是 KnowledgeService
    public ChatService(ChatHistoryRepository chatHistoryRepository,
                       OpenAiService openAiService,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper,
                       KnowledgeServiceClient knowledgeServiceClient, // 【修改】
                       ConversationRepository conversationRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.openAiService = openAiService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeServiceClient = knowledgeServiceClient; // 【修改】
        this.conversationRepository = conversationRepository;
    }

    // ... (getUserConversations, getConversationHistory, generateTitleForConversation 等方法保持不变) ...
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public Result<?> getConversationHistory(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !Objects.equals(conversation.getUserId(), userId)) {
            return Result.error(403, "无权访问该会话");
        }
        List<ChatHistory> history = chatHistoryRepository.findByConversationIdOrderByIdAsc(conversationId);
        return Result.success(history);
    }

    public Result<String> generateTitleForConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!Objects.equals(conversation.getUserId(), userId)) {
            return Result.error(403, "无权操作该会话");
        }
        List<ChatHistory> history = chatHistoryRepository.findByConversationIdOrderByIdAsc(conversationId);
        String historyText = history.stream().limit(4).map(h -> h.getRole() + ": " + h.getContent()).collect(Collectors.joining("\n"));
        String prompt = String.format("请根据以下对话内容，为这段对话生成一个简洁、不超过10个字的标题，直接返回标题内容，不要包含任何多余的解释或标点符号。\n\n对话内容：\n%s", historyText);
        final List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
        ChatCompletionRequest request = ChatCompletionRequest.builder().model(chatModelName).messages(messages).build();
        try {
            String title = openAiService.createChatCompletion(request).getChoices().get(0).getMessage().getContent().replaceAll("\"", "").trim();
            conversation.setTitle(title);
            conversationRepository.save(conversation);
            return Result.success(title);
        } catch (Exception e) {
            log.error("生成标题失败", e);
            return Result.error(500, "生成标题失败");
        }
    }

    public Result<String> simpleChat(Long userId, Long conversationId, String userQuestion) {
        String finalQuestion = getFinalQuestion(userId, userQuestion);
        String redisKey = REDIS_HISTORY_KEY_PREFIX + conversationId;
        List<ChatMessage> historyMessages = getHistoryFromRedis(redisKey);

        final List<ChatMessage> messages = new ArrayList<>(historyMessages);
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), finalQuestion));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModelName)
                .messages(messages)
                .build();
        try {
            String aiResponse = openAiService.createChatCompletion(request).getChoices().get(0).getMessage().getContent();
            saveConversationHistory(userId, conversationId, userQuestion, aiResponse);
            return Result.success(aiResponse);
        } catch (Exception e) {
            log.error("调用Ollama API失败 (simpleChat)", e);
            return Result.error(500, "AI服务调用失败: " + e.getMessage());
        }
    }
    public void streamChat(Long userId, Long conversationId, String userQuestion, SseEmitter emitter) {
        final Long finalConversationId; // 用于流式处理的最终会话ID
        // 【核心修正】: 处理新对话的逻辑
        if (conversationId == 0L) {
            Conversation newConversation = new Conversation();
            newConversation.setUserId(userId);
            newConversation.setTitle("新对话"); // 设置一个临时的标题
            Conversation savedConversation = conversationRepository.save(newConversation);
            finalConversationId = savedConversation.getId(); // 获取数据库生成的新ID
            log.info("创建新会话，ID: {}", finalConversationId);
        } else {
            finalConversationId = conversationId;
        }

        String finalQuestion = getFinalQuestion(userId, userQuestion);
        // 【核心修正】: 使用正确的会话ID（可能是新的）来构建Redis Key
        String redisKey = REDIS_HISTORY_KEY_PREFIX + finalConversationId;
        List<ChatMessage> historyMessages = getHistoryFromRedis(redisKey);

        final List<ChatMessage> messages = new ArrayList<>(historyMessages);
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), finalQuestion));

        // 【新增日志】: 打印最终发送给AI的完整消息列表
        log.info("发送给AI的完整消息列表 (会话ID: {}): {}", finalConversationId, messages);
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModelName)
                .messages(messages)
                .stream(true)
                .build();

        StringBuilder fullResponse = new StringBuilder();
        try {
            Flowable<ChatCompletionChunk> flowable = openAiService.streamChatCompletion(request);
            flowable.doOnNext(chunk -> {
                String content = chunk.getChoices().get(0).getMessage().getContent();
                if (content != null) {
                    fullResponse.append(content);
                    emitter.send(SseEmitter.event().data(content));
                }
            }).doOnComplete(() -> {
                log.info("流式输出完成");
                // 【核心修正】: 使用正确的会话ID保存历史记录
                saveConversationHistory(userId, finalConversationId, userQuestion, fullResponse.toString());
                try {
                    // 【核心修正】: 在close事件中，将正确的（可能是新的）会话ID返回给前端
                    emitter.send(SseEmitter.event().name("close").data(String.valueOf(finalConversationId)));
                } catch (IOException e) {
                    log.warn("发送close事件失败", e);
                }
                emitter.complete();
            }).doOnError(throwable -> {
                log.error("流式API调用异常", throwable);
                emitter.completeWithError(throwable);
            }).subscribe();
        } catch (Exception e) {
            log.error("发起流式请求异常", e);
            emitter.completeWithError(e);
        }
    }
    private void saveConversationHistory(Long userId, Long conversationId, String userQuestion, String aiResponse) {
        String redisKey = REDIS_HISTORY_KEY_PREFIX + conversationId;
        addHistoryToRedis(redisKey, new ChatMessage(ChatMessageRole.USER.value(), userQuestion));
        addHistoryToRedis(redisKey, new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiResponse));
        saveHistoryToDb(userId, conversationId, "user", userQuestion);
        saveHistoryToDb(userId, conversationId, "assistant", aiResponse);
    }
    private void saveHistoryToDb(Long userId, Long conversationId, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setConversationId(conversationId);
        history.setRole(role);
        history.setContent(content);
        chatHistoryRepository.save(history);
    }
    @Transactional
    public Result<?> deleteConversation(Long conversationId, Long userId) {
        // 1. 验证会话是否存在且属于当前用户
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return Result.success(); // 如果会话已不存在，也视为成功（幂等性）
        }
        if (!Objects.equals(conversation.getUserId(), userId)) {
            return Result.error(403, "无权删除该会话");
        }

        // 2. 删除数据库中的聊天记录
        chatHistoryRepository.deleteByConversationId(conversationId);

        // 3. 删除会话本身
        conversationRepository.deleteById(conversationId);

        // 4. 清理Redis缓存
        String redisKey = REDIS_HISTORY_KEY_PREFIX + conversationId;
        redisTemplate.delete(redisKey);

        log.info("用户 {} 删除了会话 {}", userId, conversationId);
        return Result.success();
    }


    private String getFinalQuestion(Long userId, String userQuestion) {
        // 【核心修改】: 不再调用本地 service，而是通过 Feign 客户端发起网络请求
        log.info("正在通过Feign客户端向knowledge-service发起远程调用...");
        List<String> relevantKnowledge = knowledgeServiceClient.search(userQuestion);
        log.info("从knowledge-service获得了 {} 条相关知识", relevantKnowledge.size());
        return buildFinalQuestionWithKnowledge(userQuestion, relevantKnowledge);
    }

    private String buildFinalQuestionWithKnowledge(String userQuestion, List<String> knowledge) {
        if (CollectionUtils.isEmpty(knowledge)) {
            return userQuestion;
        }
        String context = knowledge.stream().map(k -> "- " + k).collect(Collectors.joining("\n"));
        return String.format(
                "你是一个专业的AI客服。请根据我提供的【参考资料】，用专业、礼貌的口吻回答用户的【问题】。如果参考资料无法回答用户问题，就礼貌地告知用户你暂时无法提供这方面的信息，并建议用户联系人工客服。严禁编造答案。\n\n" +
                        "【参考资料】\n" +
                        "%s\n\n" +
                        "【问题】\n" +
                        "%s",
                context,
                userQuestion
        );
    }

    private List<ChatMessage> getHistoryFromRedis(String redisKey) {
        // 【新增日志】: 打印尝试读取的Redis Key
        log.info("正在从Redis读取历史记录, Key: {}", redisKey);
        try {
            ListOperations<String, String> ops = redisTemplate.opsForList();
            List<String> historyJson = ops.range(redisKey, 0, MAX_HISTORY_SIZE - 1);
            if (historyJson == null || historyJson.isEmpty()) {
                // 【新增日志】: 打印未找到或为空的情况
                log.info("从Redis未找到历史记录或列表为空, Key: {}", redisKey);
                return new ArrayList<>();
            }
            // 【新增日志】: 打印从Redis读取到的原始JSON条数
            log.info("从Redis成功读取到 {} 条历史记录, Key: {}", historyJson.size(), redisKey);
            return historyJson.stream().map(json -> {
                try { return objectMapper.readValue(json, ChatMessage.class); }
                catch (IOException e) { log.error("从Redis反序列化历史记录失败, item: {}", json, e); return null; }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("从Redis读取历史记录失败", e);
            return new ArrayList<>();
        }
    }

    private void addHistoryToRedis(String redisKey, ChatMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            ListOperations<String, String> ops = redisTemplate.opsForList();
            ops.rightPush(redisKey, messageJson);
            ops.trim(redisKey, -MAX_HISTORY_SIZE, -1);
            redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
            // 【新增日志】: 打印成功存入Redis的信息
            log.info("已成功向Redis存入历史记录, Key: {}, Message: {}", redisKey, messageJson);
        } catch (JsonProcessingException e) {
            log.error("向Redis序列化历史记录失败", e);
        }
    }
}