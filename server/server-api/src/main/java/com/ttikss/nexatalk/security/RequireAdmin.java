package com.ttikss.nexatalk.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要管理员权限的接口
 * 配合 @CurrentUser 使用，表示需要管理员身份才能访问
 *
 * @value 指定需要的最小角色等级：
 *   1 = 管理员
 *   2 = 超级管理员（包含管理员权限）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
    /**
     * 需要的最小角色等级，默认为 1（管理员）
     */
    int value() default 1;
}
