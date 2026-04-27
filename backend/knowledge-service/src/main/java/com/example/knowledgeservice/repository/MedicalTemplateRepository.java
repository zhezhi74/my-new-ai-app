package com.example.knowledgeservice.repository;

import com.example.knowledgeservice.entity.MedicalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalTemplateRepository extends JpaRepository<MedicalTemplate, Long> {

    // 如果以后前端需要根据科室筛选模板，可以用这个方法
    List<MedicalTemplate> findByCategory(String category);
}