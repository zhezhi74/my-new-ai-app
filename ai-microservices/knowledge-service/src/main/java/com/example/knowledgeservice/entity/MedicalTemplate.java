package com.example.knowledgeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * 医学临床报告模板实体类
 * 作用：为前端多模态报告引擎提供固定的排版格式 (非 RAG 检索数据)
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "medical_template")
public class MedicalTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 模板名称，如："SOAP 标准门诊病历"、"心血管超声报告单"
    @Column(nullable = false)
    private String name;

    // 模板的结构定义（通常存 JSON 字符串，告诉前端如何渲染表单）
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String structure;

    // 适用科室/分类，如："全科"、"放射科"
    @Column(nullable = false)
    private String category;

    private Date createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
    }
}