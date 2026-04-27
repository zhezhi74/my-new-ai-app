package com.example.chatservice.entity;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "chat_history")
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // 角色： "user" 或 "assistant"
    private String role;

    @Lob // 表示这是一个大文本字段
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long conversationId; // <-- 【新增】

    private Date createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
    }
}