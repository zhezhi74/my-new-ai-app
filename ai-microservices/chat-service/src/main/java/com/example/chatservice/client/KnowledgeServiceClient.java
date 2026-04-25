package com.example.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "knowledge-service", url = "http://localhost:8082") // 指向知识库服务
public interface KnowledgeServiceClient {

    @PostMapping("/api/knowledge/search")
    List<String> search(@RequestBody String query);
}