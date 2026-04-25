package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/users") // 使用 internal 路径以区分
public class InternalUserController {

    @Autowired
    private UserRepository userRepository;

    // 提供一个根据ID列表批量查询用户的接口
    @PostMapping("/find-by-ids")
    public List<User> findUsersByIds(@RequestBody List<Long> userIds) {
        return userRepository.findAllById(userIds);
    }
}