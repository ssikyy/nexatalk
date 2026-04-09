package com.ttikss.nexatalk.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.security.LoginUser;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.OperationLogService;
import com.ttikss.nexatalk.util.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Controller 请求日志切面（增强版）
 *
 * 作用：
 * - 拦截所有 Controller 方法的执行
 * - 自动记录：请求方法、URL、当前用户、耗时、响应状态
 * - 将日志持久化到数据库（operation_log 表）
 * - 异常时记录异常信息
 */
@Aspect
@Component
public class RequestLogAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public RequestLogAspect(OperationLogService operationLogService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.ttikss.nexatalk.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String method = "UNKNOWN";
        String url = "UNKNOWN";
        String ip = "";

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            method = request.getMethod();
            url = request.getRequestURI();
            ip = getClientIp(request);
        }

        LoginUser loginUser = UserContext.get();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        String username = loginUser != null ? loginUser.getUsername() : "";

        String module = extractModule(url);
        String operation = joinPoint.getSignature().getName();

        String params = LogSanitizer.sanitizeForLog(objectMapper, joinPoint.getArgs());

        Object result = null;
        Integer status = 1;
        String errorMsg = "";
        String resultStr = "";

        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[REQUEST] {} {} userId={} username={} | 耗时: {}ms", method, url, userId, username, elapsed);

            resultStr = LogSanitizer.sanitizeForLog(objectMapper, result);

            return result;
        } catch (Throwable e) {
            status = 0;
            errorMsg = e.getMessage();
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("[REQUEST] {} {} userId={} username={} | 耗时: {}ms | 异常: {}: {}",
                    method, url, userId, username, elapsed,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            operationLogService.log(
                    userId,
                    username,
                    module,
                    operation,
                    method,
                    ip,
                    params,
                    resultStr,
                    status,
                    errorMsg,
                    (int) (System.currentTimeMillis() - startTime)
            );
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String extractModule(String url) {
        if (url == null || url.isEmpty()) return "";

        // 忽略无关接口
        if (url.contains("/health") || url.contains("/actuator")
                || url.contains("/api/ping")
                || url.contains("/swagger")
                || url.contains("/v3/api-docs")
                || url.contains("/doc.html")) {
            return null; // 不记录这些接口
        }

        String normalizedUrl = url.startsWith("/") ? url.substring(1) : url;
        if (normalizedUrl.startsWith("api/admin/")) {
            String[] parts = normalizedUrl.split("/");
            if (parts.length >= 3) {
                return "admin:" + parts[2];
            }
        }
        if (normalizedUrl.startsWith("api/")) {
            String[] parts = normalizedUrl.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return normalizedUrl;
    }
}
