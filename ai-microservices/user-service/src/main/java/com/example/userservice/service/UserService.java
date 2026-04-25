package com.example.userservice.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.example.userservice.common.Result;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public Result<?> register(LoginRequest request) {
        if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
            return Result.error(400, "用户名或密码不能为空");
        }

        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            return Result.error(400, "用户名已存在");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        // 使用Hutool进行SHA-256加密
        String hashedPassword = DigestUtil.sha256Hex(request.getPassword());
        newUser.setPassword(hashedPassword);

        userRepository.save(newUser);
        return Result.success();
    }

    public Result<?> login(LoginRequest request) {
        if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
            return Result.error(400, "用户名或密码不能为空");
        }

        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        if (!userOptional.isPresent()) {
            return Result.error(401, "用户名或密码错误");
        }

        User user = userOptional.get();
        String hashedPassword = DigestUtil.sha256Hex(request.getPassword());

        if (!user.getPassword().equals(hashedPassword)) {
            return Result.error(401, "用户名或密码错误");
        }

        // 【修改】: 生成Token时，传入role
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> response = new HashMap<>(); // <--【优化】value改为Object
        response.put("token", token);
        response.put("role", user.getRole()); // 同时在登录成功时直接返回角色

        return Result.success(response);
    }
}