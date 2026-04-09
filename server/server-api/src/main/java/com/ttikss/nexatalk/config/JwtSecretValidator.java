package com.ttikss.nexatalk.config;

import com.ttikss.nexatalk.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 启动时校验 JWT 密钥配置。
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    public JwtSecretValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        boolean usingDefaultSecret = !StringUtils.hasText(jwtSecret)
                || JwtUtil.DEFAULT_SECRET.equals(jwtSecret);
        boolean prodProfileActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));

        if (prodProfileActive && usingDefaultSecret) {
            throw new IllegalStateException("生产环境必须通过 JWT_SECRET 配置安全的 JWT 密钥");
        }

        if (usingDefaultSecret) {
            log.warn("当前使用默认 JWT 密钥，仅适用于本地开发环境");
        }
    }
}
