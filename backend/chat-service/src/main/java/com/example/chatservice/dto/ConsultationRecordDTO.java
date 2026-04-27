package com.example.chatservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConsultationRecordDTO {
    private String id;
    private String username;
    private String time;
    private String desc;
    private List<String> tags;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private List<String> logs;
}