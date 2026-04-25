package com.example.userservice.controller;

import com.example.userservice.common.Result;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody LoginRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }
}