package com.ttikss.nexatalk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * 职责：
 * - 生成 Access Token（登录时调用）
 * - 解析并校验 Token（拦截器中调用）
 * - 从 Token 中提取用户 ID 和角色
 *
 * Token 设计：
 * - Subject：用户 ID（字符串形式）
 * - Claim "role"：用户角色（0=普通用户, 1=管理员）
 * - Token 存储在 Redis 中，支持主动登出（删 Redis key 即失效）
 */
@Component
public class JwtUtil {

    public static final String DEFAULT_SECRET = "NexaTalkDefaultSecretKeyForDevelopment2024MustBeAtLeast256Bits";

    /** JWT 签名密钥（从配置文件读取，生产环境由环境变量注入） */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 有效期（毫秒） */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 获取 HMAC-SHA256 签名密钥
     * 每次调用时根据配置生成，避免 static 初始化顺序问题
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param role     用户角色（0=普通用户, 1=管理员）
     * @param username 用户名（用于日志追踪）
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, int role, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                // Subject 存用户 ID，方便后续取用
                .subject(String.valueOf(userId))
                // 自定义 Claim 存角色
                .claim("role", role)
                // 自定义 Claim 存用户名（用于日志追踪）
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 JWT Token（兼容旧版本，无用户名）
     *
     * @param userId 用户 ID
     * @param role   用户角色（0=普通用户, 1=管理员）
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, int role) {
        return generateToken(userId, role, "");
    }

    /**
     * 解析 Token，提取所有 Claims
     * 若 Token 非法或已过期，抛出 JwtException
     *
     * @param token JWT 字符串（不含 "Bearer " 前缀）
     * @return Claims 对象
     * @throws JwtException Token 非法或过期
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 从 Token 中提取用户角色
     *
     * @param token JWT 字符串
     * @return 角色值（0=普通用户, 1=管理员）
     */
    public int getRole(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    /**
     * 从 Token 中提取用户名
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 校验 Token 是否合法（签名正确 + 未过期）
     *
     * @param token JWT 字符串
     * @return true=合法, false=非法或过期
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
