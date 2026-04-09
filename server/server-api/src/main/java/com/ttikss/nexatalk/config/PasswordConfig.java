package com.ttikss.nexatalk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码配置类（PasswordConfig）
 *
 * 这个类做什么：
 * - 提供 PasswordEncoder（BCrypt 算法）
 * - 注册时用它加密密码；登录时用它校验密码
 *
 * 为什么必须加密：
 * - 数据库永远不存明文密码（这是最基本的安全要求）
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt：业内最常用的密码哈希算法之一（带盐、可抗彩虹表）
        return new BCryptPasswordEncoder();
    }
}