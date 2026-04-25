package com.example.chatservice.client;

import com.example.chatservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;


@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserServiceClient {

    @PostMapping("/api/internal/users/find-by-ids")
    List<UserDTO> findUsersByIds(@RequestBody List<Long> userIds);
}