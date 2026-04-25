package com.example.chatservice.dto;
import lombok.Data;
import java.util.Date;

@Data
public class ChatHistoryDto {
    private Long id;
    private String username;
    private String role;
    private String content;
    private Long userId;
    private Date createTime;
}