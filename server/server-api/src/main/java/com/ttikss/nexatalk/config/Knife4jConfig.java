package com.ttikss.nexatalk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（Swagger）API 文档配置
 *
 * 访问地址（启动后可用）：
 *   http://localhost:8080/doc.html    —— Knife4j 增强 UI
 *   http://localhost:8080/swagger-ui/index.html —— 原版 Swagger UI
 *
 * 安全方案：
 * - 配置了 Bearer Token 认证方案
 * - 在 Knife4j 页面点击右上角"Authorize"，粘贴 Token 即可自动注入请求头
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI nexaTalkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexaTalk API 文档")
                        .description("NexaTalk 社交平台后端接口文档\n\n" +
                                "**认证说明**：需要登录的接口请先调用 POST /api/users/login 获取 Token，\n" +
                                "然后点击右上角 Authorize 按钮，输入 `Bearer {token}` 后点击确认。")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("NexaTalk Team")
                                .email("dev@nexatalk.com")))
                // 全局安全方案：Bearer Token（JWT）
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token 认证，格式：Bearer {token}")));
    }
}
