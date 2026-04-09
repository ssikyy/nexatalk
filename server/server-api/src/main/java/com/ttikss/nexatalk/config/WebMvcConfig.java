package com.ttikss.nexatalk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.security.CurrentUserArgumentResolver;
import com.ttikss.nexatalk.security.JwtAuthInterceptor;
import com.ttikss.nexatalk.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 全局配置
 *
 * 职责：
 * 1. 注册 JWT 鉴权拦截器，并设置放行路径
 * 2. 注册 @CurrentUser 参数解析器
 * 3. 配置跨域规则（CORS），允许前端开发时跨域访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String uploadPath;

    public WebMvcConfig(JwtUtil jwtUtil,
                        StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        @Value("${app.upload.path}") String uploadPath) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.uploadPath = uploadPath;
    }

    /**
     * 注册 JWT 软鉴权拦截器
     *
     * 设计变更：拦截器改为"软鉴权"模式
     * - 有 Token：解析并写入 UserContext
     * - 无 Token：匿名通行，由 @CurrentUser 解析器在需要时抛出 401
     * 好处：无需维护路径白名单，接口级粒度由注解控制
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtUtil, redisTemplate, objectMapper))
                // 拦截所有路径（包括 /api/**），解析 Token（有则用，无则跳过）
                .addPathPatterns("/**")
                // 仍排除 Knife4j/Swagger 文档路径，避免不必要的解析
                .excludePathPatterns("/doc.html")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/swagger-resources/**")
                .excludePathPatterns("/webjars/**");
    }

    /**
     * 注册 @CurrentUser 注解的参数解析器
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    /**
     * 配置 CORS 跨域
     * 开发阶段允许所有来源，生产环境应限制为具体域名
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 生产环境改为实际域名，如 https://nexatalk.com
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 允许携带 Cookie（JWT 用 Header，此处保留以备扩展）
                .allowCredentials(true)
                .maxAge(3600);

        // 允许访问上传的文件（静态资源）
        registry.addMapping("/uploads/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源访问
     * 将 /uploads/** 映射到应用运行目录下的 uploads 文件夹
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedUploadPath = java.nio.file.Paths.get(uploadPath)
                .toAbsolutePath()
                .normalize()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizedUploadPath + "/");
    }
}
