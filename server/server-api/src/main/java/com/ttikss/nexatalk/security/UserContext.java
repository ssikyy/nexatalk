package com.ttikss.nexatalk.security;

/**
 * 用户上下文持有器（ThreadLocal 工具类）
 *
 * 作用：在请求处理线程中存储和读取当前登录用户信息。
 * 生命周期：
 *   1. JwtAuthInterceptor.preHandle()  → set(loginUser)
 *   2. Controller 处理请求             → get() 获取用户
 *   3. JwtAuthInterceptor.afterCompletion() → clear() 防内存泄漏
 */
public final class UserContext {

    /** 私有构造，工具类不允许实例化 */
    private UserContext() {}

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登录用户（由拦截器调用）
     */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /**
     * 获取当前登录用户，未登录则返回 null
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID，未登录则返回 null
     */
    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取当前用户角色，未登录则返回 -1
     */
    public static int getRole() {
        LoginUser loginUser = HOLDER.get();
        return loginUser != null ? loginUser.getRole() : -1;
    }

    /**
     * 清除上下文（必须在请求结束时调用，防止 ThreadLocal 内存泄漏）
     */
    public static void clear() {
        HOLDER.remove();
    }
}
