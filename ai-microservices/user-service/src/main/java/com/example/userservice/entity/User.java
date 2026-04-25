package com.example.userservice.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Data // Lombok注解，自动生成getter, setter, toString等方法
@Table(name = "user") // 指定数据库表名
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增
    private Long id;

    @Column(unique = true, nullable = false) // 用户名唯一且不为空
    private String username;

    @Column(nullable = false)
    private String password;

    private Date createTime;


    @Column(nullable = false)
    private String role;

    @PrePersist // 在数据持久化（插入）之前自动调用
    protected void onCreate() {
        this.createTime = new Date();
        // 为新注册的用户设置默认角色
        if (this.role == null) {
            this.role = "user";
        }
    }
}