package com.ttikss.nexatalk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.common.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器
 *
 * 处理流程：
 * 1. 从 Authorization 请求头中取出 Token（格式：Bearer <token>）
 * 2. 校验 Token 签名和过期时间
 * 3. 检查 Redis 中 Token 是否有效（支持主动登出：删 Redis key = Token 失效）
 * 4. 将用户信息写入 UserContext（ThreadLocal）
 * 5. 请求结束后清除 UserContext，防内存泄漏
 *
 * 拦截路径在 WebMvcConfig 中配置：
 *   - 拦截：/api/** （需要登录的业务接口）
 *   - 放行：/api/users/register、/api/users/login、/health、/api/ping 等
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    /** Redis 中 Token 的 key 前缀：token:{userId} */
    private static final String TOKEN_KEY_PREFIX = "token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtUtil jwtUtil,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws IOException {
        RequireAdmin requireAdmin = getRequireAdmin(handler);

        // 1. 从请求头提取 Token
        String authHeader = request.getHeader("Authorization");

        // 没有携带 Token：公开接口放行；管理员接口直接拦截
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            if (requireAdmin != null) {
                writeUnauthorized(response, "请先登录");
                return false;
            }
            return true;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // 2. 校验 JWT 签名和过期时间
        Long userId;
        int role;
        String username;
        try {
            userId = jwtUtil.getUserId(token);
            role = jwtUtil.getRole(token);
            username = jwtUtil.getUsername(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token 格式非法：返回 401，让客户端重新登录
            writeUnauthorized(response, "Token 非法或已过期，请重新登录");
            return false;
        }

        // 3. 校验 Redis：检查 Token 是否已被主动登出（logout 时删除对应 key）
        String redisKey = TOKEN_KEY_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (!token.equals(storedToken)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 4. 将用户信息写入 ThreadLocal，供后续 Controller 使用
        LoginUser loginUser = new LoginUser(userId, role, username);
        UserContext.set(loginUser);

        if (requireAdmin != null && loginUser.getRole() < requireAdmin.value()) {
            writeForbidden(response, requireAdmin.value() >= 2 ? "仅超级管理员可访问" : "仅管理员可访问");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 Exception ex) {
        // 5. 请求结束后清除 ThreadLocal，防止内存泄漏（多实例/线程池场景下尤为重要）
        UserContext.clear();
    }

    /**
     * 写出 401 响应，返回统一 JSON 格式
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ErrorCode.UNAUTHORIZED.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ErrorCode.FORBIDDEN.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private RequireAdmin getRequireAdmin(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequireAdmin methodAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireAdmin.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireAdmin.class);
    }
}
