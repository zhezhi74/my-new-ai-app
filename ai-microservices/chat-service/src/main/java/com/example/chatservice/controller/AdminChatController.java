package com.example.chatservice.controller;

import com.example.chatservice.dto.UserDTO;
import com.example.chatservice.client.UserServiceClient;
import com.example.chatservice.dto.ChatHistoryDto;
import com.example.chatservice.entity.ChatHistory;
import com.example.chatservice.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminChatController {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryDto>> getAllChatHistory(@RequestParam(required = false) String username) {
        List<ChatHistory> allHistory = chatHistoryRepository.findAll();
        if (allHistory.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Long> userIds = allHistory.stream()
                .map(ChatHistory::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 【核心修正】: 使用 getter 方法 (getId, getUsername) 代替直接访问私有字段
        Map<Long, String> userMap = userServiceClient.findUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getUsername));

        List<ChatHistoryDto> dtos = allHistory.stream().map(history -> {
            ChatHistoryDto dto = new ChatHistoryDto();
            dto.setId(history.getId());
            dto.setRole(history.getRole());
            dto.setContent(history.getContent());
            dto.setCreateTime(history.getCreateTime());
            dto.setUserId(history.getUserId());

            if ("assistant".equals(history.getRole())) {
                dto.setUsername("AI助手");
            } else {
                dto.setUsername(userMap.getOrDefault(history.getUserId(), "未知用户"));
            }
            return dto;
        }).collect(Collectors.toList());

        if (StringUtils.hasText(username)) {
            dtos = dtos.stream()
                    .filter(dto -> username.equals(dto.getUsername()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(dtos);
    }
}