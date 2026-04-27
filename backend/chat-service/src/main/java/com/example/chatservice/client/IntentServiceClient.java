package com.example.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 医疗意图识别微服务 (Python FastAPI) 的 Feign 客户端
 * 架构意义：实现 Classify-then-Retrieve 架构中的 "Classify" 路由功能
 */
@FeignClient(name = "intent-service", url = "http://127.0.0.1:8000")
public interface IntentServiceClient {

    @PostMapping("/api/intent/predict")
    Map<String, Object> predictIntent(@RequestBody Map<String, String> request);
}