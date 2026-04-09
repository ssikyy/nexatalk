package com.ttikss.nexatalk.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前登录用户注解
 *
 * 用法：在 Controller 方法参数上标注 @CurrentUser，
 * Spring 会自动从请求上下文中注入当前登录用户 ID。
 *
 * 示例：
 *   public Result<?> createPost(@CurrentUser Long userId, @RequestBody PostRequest req)
 *
 * 原理：由 CurrentUserArgumentResolver 解析，从 ThreadLocal 中取 LoginUser 对象
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
