package com.example.knowledgeservice.controller;

import com.example.knowledgeservice.entity.Knowledge;
import com.example.knowledgeservice.repository.KnowledgeRepository;
import com.example.knowledgeservice.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private KnowledgeService knowledgeService;

    // 1. 获取所有知识
    @GetMapping
    public ResponseEntity<List<Knowledge>> getAllKnowledge() {
        // 为了让路径更符合RESTful风格，这个直接用根路径 /
        return ResponseEntity.ok(knowledgeRepository.findAll());
    }

    // 2. 添加新知识
    @PostMapping("/add")
    public ResponseEntity<String> addKnowledge(@RequestBody Knowledge knowledge) {
        if (knowledge.getTitle() == null || knowledge.getTitle().isEmpty() ||
                knowledge.getContent() == null || knowledge.getContent().isEmpty()) {
            return ResponseEntity.badRequest().body("标题和内容均不能为空");
        }
        try {
            knowledgeService.addKnowledge(knowledge);
            return ResponseEntity.ok("Knowledge added successfully!");
        } catch (Exception e) {
            log.error("添加知识失败", e);
            return ResponseEntity.internalServerError().body("Failed to add knowledge: " + e.getMessage());
        }
    }

    // 3. 根据ID删除知识
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteKnowledge(@PathVariable Long id) {
        try {
            knowledgeService.deleteKnowledge(id);
            return ResponseEntity.ok("Knowledge deleted successfully!");
        } catch (Exception e) {
            log.error("删除知识失败, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete knowledge.");
        }
    }

    // 4. 更新知识
    @PutMapping
    public ResponseEntity<String> updateKnowledge(@RequestBody Knowledge knowledge) {
        // 使用 PUT 方法，路径为资源本身
        try {
            knowledgeService.updateKnowledge(knowledge);
            return ResponseEntity.ok("Knowledge updated successfully!");
        } catch (Exception e) {
            log.error("更新知识失败, ID: {}", knowledge.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update knowledge.");
        }
    }

    //一个用于服务间调用的搜索接口
    @PostMapping("/search")
    public ResponseEntity<List<String>> searchRelevantKnowledge(@RequestBody String query) {
        // 直接调用重构后的双轨检索方法
        // 该方法内部已自动实现了：Top 3 医学事实 + Top 2 专家思维链
        List<String> results = knowledgeService.search(query);
        return ResponseEntity.ok(results);
    }
}