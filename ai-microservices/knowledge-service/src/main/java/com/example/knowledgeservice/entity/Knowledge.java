package com.example.knowledgeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor  // 【新增】：生成无参构造函数
@AllArgsConstructor // 【新增】：生成全参构造函数（现在包含 5 个参数）
@Table(name = "knowledge")
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //知识标题字段
    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // =====================================
    // 【核心新增】：知识分类，用于触发双轨智能路由
    // =====================================
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT '默认分类'")
    private String category;

    private Date createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
    }
}