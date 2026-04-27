package com.example.chatservice.repository;

import com.example.chatservice.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    // 根据用户ID查询聊天记录，并按ID升序排序
    List<ChatHistory> findByUserIdOrderByIdAsc(Long userId);
    // 根据会话ID查找历史记录
    List<ChatHistory> findByConversationIdOrderByIdAsc(Long conversationId);
    // 根据会话ID删除所有聊天记录
    void deleteByConversationId(Long conversationId);
}