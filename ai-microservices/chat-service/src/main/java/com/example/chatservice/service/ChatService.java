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
import org.springframework.beans.factory.annotation.Qualifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.example.chatservice.client.IntentServiceClient;
import java.util.HashMap;
import java.util.Map;

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
    // 【新增】注入意图识别客户端
    private final IntentServiceClient intentServiceClient;
    private final ConversationRepository conversationRepository;

    @Value("${ai.deepseek.chat-model}")
    private String chatModelName;

    // 【核心修改】: 修改构造函数，注入 KnowledgeServiceClient 而不是 KnowledgeService
    public ChatService(ChatHistoryRepository chatHistoryRepository,
                       @Qualifier("deepSeekChatService")OpenAiService openAiService,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper,
                       KnowledgeServiceClient knowledgeServiceClient, // 【修改】
                       IntentServiceClient intentServiceClient, // 【新增】
                       ConversationRepository conversationRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.openAiService = openAiService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeServiceClient = knowledgeServiceClient;
        this.intentServiceClient = intentServiceClient;// 【修改】
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


    // 【彻底重写】getFinalQuestion 方法
    private String getFinalQuestion(Long userId, String userQuestion) {
        log.info("============= [路由中枢启动] =============");
        int intentCode = 2; // 默认防身策略：2 (闲聊)，防止 Python 挂了导致整个系统崩溃
        String intentLabel = "Unknown";

        // 1. 请求 Python 分诊台，进行细粒度意图识别
        try {
            Map<String, String> requestPayload = new HashMap<>();
            requestPayload.put("query", userQuestion);

            log.info("--> [步骤1] 调用 BERT 意图微服务...");
            Map<String, Object> response = intentServiceClient.predictIntent(requestPayload);

            if (response != null && (Integer) response.get("code") == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                intentCode = (Integer) data.get("intent_code");
                intentLabel = (String) data.get("intent_label");
            }
        } catch (Exception e) {
            log.error("意图识别微服务调用失败，自动降级为【闲聊】处理", e);
        }

        log.info("<-- 意图识别完毕: 类别=[{}], 标签=[{}]", intentCode, intentLabel);

        // 2. 根据识别到的意图，执行动态路由策略
        if (intentCode == 2) {
            // 【策略 A：闲聊】 - 绕过 RAG 检索，极速响应
            log.info("--> [步骤2] 判定为【闲聊】或通用提问，跳过 Weaviate 向量库检索。");
            return buildChitchatPrompt(userQuestion);
        } else {
            // 【策略 B：医疗咨询】 - 触发 RAG 检索 (目前复用旧接口，未来可拆分为双维检索)
            log.info("--> [步骤2] 判定为【严谨医疗】意图，向 knowledge-service 发起事实库检索...");
            List<String> relevantKnowledge = knowledgeServiceClient.search(userQuestion);
            log.info("<-- 检索完毕，召回 {} 条客观医学事实", relevantKnowledge.size());

            return buildMedicalPrompt(userQuestion, relevantKnowledge, intentCode);
        }
    }

    // 【新增】纯闲聊的 Prompt 模板
    private String buildChitchatPrompt(String userQuestion) {
        return "你是一个具备丰富医学知识但同时非常平易近人的AI医生。用户现在想和你进行一些日常交流或基础提问，不需要进行深度的病理推导。" +
                "\n请用友好、温暖的语气回答以下用户内容：\n\n【用户内容】：\n" + userQuestion;
    }

    // 【修改】带有严格约束的医疗 Prompt 模板 (融合了你论文中的初步 CoT 思想)
    private String buildMedicalPrompt(String userQuestion, List<String> knowledge, int intentCode) {
        String context = CollectionUtils.isEmpty(knowledge)
                ? "（无向量库匹配事实，请依据基础医学常识谨慎回答）"
                : knowledge.stream().map(k -> "- " + k).collect(Collectors.joining("\n"));

        // 根据细粒度意图 (0: 诊断, 1: 用药) 注入不同的基础思维链指令
        String cotInstruction = (intentCode == 0)
                ? "【诊断推理要求】：1. 分析可能诱发该症状的常见病因。2. 评估症状的严重性。3. 给出是否需要立即线下就医的明确建议。"
                : "【用药建议要求】：1. 强调药物的适用症。2. 提醒常见副作用与禁忌症。3. 声明AI不能代替处方，必须遵医嘱。";

        return String.format(
                "你是一个极其严谨的主治医师。请务必基于提供的【医学客观事实】来回答患者的【病情描述】。\n\n" +
                        "【严格约束】：\n" +
                        "1. 绝不允许编造不存在的药物或治疗方案。\n" +
                        "2. 你的回答必须包含以下逻辑环节：%s\n\n" +
                        "【医学客观事实】\n%s\n\n" +
                        "【病情描述】\n%s",
                cotInstruction, context, userQuestion
        );
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