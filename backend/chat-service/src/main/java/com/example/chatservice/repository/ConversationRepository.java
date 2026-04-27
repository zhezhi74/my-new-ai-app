package com.example.chatservice.repository;

import com.example.chatservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    // 根据用户ID查找他所有的会话，按创建时间降序
    List<Conversation> findByUserIdOrderByCreateTimeDesc(Long userId);
}