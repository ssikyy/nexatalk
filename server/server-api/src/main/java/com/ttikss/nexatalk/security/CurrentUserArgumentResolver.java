package com.ttikss.nexatalk.security;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUser 注解参数解析器
 *
 * 作用：当 Controller 方法参数带有 @CurrentUser 时，
 * 自动从 UserContext（ThreadLocal）中取出当前登录用户 ID 注入。
 *
 * 与拦截器的配合：
 * - 拦截器为"软鉴权"：有 Token 则解析，无 Token 则匿名通行
 * - 本解析器为"强鉴权"：@CurrentUser 参数如果取不到 userId，直接抛 401
 *   从而实现"接口级"的登录校验，不再依赖路径白名单维护
 *
 * 管理员校验：
 * - 如果 Controller 方法还标注了 @RequireAdmin，则同时校验角色
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 判断是否支持该参数：参数带有 @CurrentUser 且类型为 Long
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * 解析参数值：从 UserContext 中取当前用户 ID
     * 未登录时（UserContext 为空）直接抛出 401 业务异常
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.code(), "请先登录");
        }

        // 如果方法标注了 @RequireAdmin，校验角色等级
        var method = parameter.getMethod();
        if (method != null && method.isAnnotationPresent(RequireAdmin.class)) {
            RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);
            int requiredLevel = requireAdmin.value();
            int userRole = loginUser.getRole();
            // 角色等级：0=普通用户, 1=管理员, 2=超级管理员
            if (userRole < requiredLevel) {
                String msg = requiredLevel >= 2 ? "仅超级管理员可访问" : "仅管理员可访问";
                throw new BusinessException(ErrorCode.FORBIDDEN.code(), msg);
            }
        }

        return loginUser.getUserId();
    }
}
