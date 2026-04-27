package com.example.chatservice.controller;

import com.example.chatservice.common.Result;
import com.example.chatservice.entity.Conversation;
import com.example.chatservice.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 确保导入 RequestHeader
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<?> getHistory(@PathVariable Long conversationId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(chatService.getConversationHistory(conversationId, userId));
    }

    @PostMapping("/send")
    public Result<String> sendMessage(@RequestBody Map<String, Object> payload,
                                      @RequestHeader("X-User-Id") Long userId) {
        String question = (String) payload.get("question");
        Long conversationId = payload.get("conversationId") != null ? ((Number) payload.get("conversationId")).longValue() : 0L;
        return chatService.simpleChat(userId, conversationId, question);
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String question,
                             @RequestParam Long conversationId,
                             @RequestHeader("X-User-Id") Long userId) {
        SseEmitter emitter = new SseEmitter(3600_000L);
        chatService.streamChat(userId, conversationId, question, emitter);
        return emitter;
    }

    @PostMapping("/title")
    public Result<String> generateTitle(@RequestBody Map<String, Long> payload,
                                        @RequestHeader("X-User-Id") Long userId) {
        Long conversationId = payload.get("conversationId");
        return chatService.generateTitleForConversation(conversationId, userId);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Result<?> deleteConversation(@PathVariable Long conversationId,
                                        @RequestHeader("X-User-Id") Long userId) {
        return chatService.deleteConversation(conversationId, userId);
    }
}