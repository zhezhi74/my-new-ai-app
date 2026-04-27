package com.example.knowledgeservice.controller;

import com.example.knowledgeservice.entity.MedicalTemplate;
import com.example.knowledgeservice.repository.MedicalTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/templates")
public class MedicalTemplateController {

    @Autowired
    private MedicalTemplateRepository templateRepository;

    // 1. 获取所有临床报告模板 (给多模态报告引擎的下拉框用)
    @GetMapping
    public ResponseEntity<List<MedicalTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    // 2. 根据科室分类获取模板
    @GetMapping("/category/{category}")
    public ResponseEntity<List<MedicalTemplate>> getTemplatesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(templateRepository.findByCategory(category));
    }

    // 3. 新增一个报告模板 (后台管理用)
    @PostMapping("/add")
    public ResponseEntity<String> addTemplate(@RequestBody MedicalTemplate template) {
        try {
            templateRepository.save(template);
            return ResponseEntity.ok("临床报告模板创建成功！");
        } catch (Exception e) {
            log.error("模板创建失败", e);
            return ResponseEntity.internalServerError().body("创建失败: " + e.getMessage());
        }
    }
}