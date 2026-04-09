package com.ttikss.nexatalk.security;

/**
 * 登录用户上下文对象
 *
 * 存储在 ThreadLocal 中，贯穿整个请求生命周期。
 * 由 JwtAuthInterceptor 在请求进入时解析 Token 并填充，
 * 请求结束时自动清除（防内存泄漏）。
 */
public class LoginUser {

    /** 当前登录用户 ID */
    private final Long userId;

    /** 当前用户角色：0=普通用户, 1=管理员 */
    private final int role;

    /** 当前用户名（用于日志追踪） */
    private final String username;

    public LoginUser(Long userId, int role) {
        this(userId, role, "");
    }

    public LoginUser(Long userId, int role, String username) {
        this.userId = userId;
        this.role = role;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public int getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    /** 判断是否为管理员（包括超级管理员） */
    public boolean isAdmin() {
        return role >= 1;
    }

    /** 判断是否为超级管理员 */
    public boolean isSuperAdmin() {
        return role == 2;
    }
}
