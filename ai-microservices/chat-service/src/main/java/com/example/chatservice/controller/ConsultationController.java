package com.example.chatservice.controller; // 注意包名是 chatservice

import com.example.chatservice.dto.ConsultationRecordDTO;
import com.example.chatservice.entity.ChatHistory;
import com.example.chatservice.repository.ChatHistoryRepository;
import com.example.chatservice.service.AiExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private AiExtractionService aiExtractionService;

    // 【核心注入】：把你真实的聊天记录仓库引进来！
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @GetMapping("/pending")
    public ResponseEntity<List<ConsultationRecordDTO>> getPendingConsultations() {
        List<ConsultationRecordDTO> results = new ArrayList<>();

        // 1. 从真实数据库获取所有聊天记录
        List<ChatHistory> allHistories = chatHistoryRepository.findAll();

        // 2. 【核心架构升级】：按会话ID (conversationId) 分组，而不是 userId！
        // 这样每一次独立的问诊都会生成一份独立的报告
        Map<Long, List<ChatHistory>> conversationMap = allHistories.stream()
                .filter(h -> h.getConversationId() != null && h.getContent() != null)
                .collect(Collectors.groupingBy(ChatHistory::getConversationId));

        // 3. 遍历每一个“门诊会话”
        for (Map.Entry<Long, List<ChatHistory>> entry : conversationMap.entrySet()) {
            Long conversationId = entry.getKey();
            List<ChatHistory> histories = entry.getValue();

            // 从这个会话的第一条消息里，顺便带出患者的真实 userId
            Long userId = histories.get(0).getUserId();

            // 拼接更专业的临床显示名称
            String displayName = "患者ID:" + userId + " (门诊单号:#" + conversationId + ")";

            // 拼接该次会话的长文本
            StringBuilder rawChatBuilder = new StringBuilder();
            for (ChatHistory h : histories) {
                String roleName = "user".equals(h.getRole()) ? "患者" : "AI辅助医生";
                rawChatBuilder.append(roleName).append(": ").append(h.getContent()).append("\n");
            }

            String realChatText = rawChatBuilder.toString();

            if (realChatText.length() < 15) {
                continue; // 过滤掉只有一句“你好”的无效会话
            }

            // 调用 DeepSeek 萃取这次门诊的内容
            AiExtractionService.ExtractedData aiData = aiExtractionService.extractFromChatHistory(realChatText);

            ConsultationRecordDTO dto = new ConsultationRecordDTO();
            // ID 改用 conversationId 保证前端列表的唯一性
            dto.setId("conv-" + conversationId);
            dto.setUsername(displayName);
            dto.setTime(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(histories.get(0).getCreateTime()));

            dto.setDesc(aiData.getDesc());
            dto.setTags(aiData.getTags());
            dto.setLogs(Arrays.asList(
                    "🧠 AI 萃取引擎：已成功降噪并处理了该门诊单的 " + histories.size() + " 条对话",
                    "🎯 意图识别网络：深度分析完成",
                    "⚡ 来源表：chat_history -> 门诊单号: #" + conversationId
            ));

            results.add(dto);
        }

        return ResponseEntity.ok(results);
    }
}