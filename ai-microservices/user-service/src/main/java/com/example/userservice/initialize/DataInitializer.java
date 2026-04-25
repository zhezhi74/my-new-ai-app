package com.example.userservice.initialize;

import cn.hutool.crypto.digest.DigestUtil;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查 "zhezhi74" 这个管理员用户是否已存在
        if (!userRepository.findByUsername("zhezhi74").isPresent()) {
            User adminUser = new User();

            adminUser.setUsername("zhezhi74");

            adminUser.setPassword(DigestUtil.sha256Hex("123456"));

            // 关键：设置角色为 "admin"
            adminUser.setRole("admin");

            userRepository.save(adminUser);
            System.out.println(">>> 默认管理员用户 'zhezhi74' 创建成功!");
        }
    }
}