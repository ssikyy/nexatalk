package com.ttikss.nexatalk.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪 ID 过滤器（TraceId）
 *
 * 作用：
 * - 为每个 HTTP 请求生成唯一的 traceId，写入 MDC（Mapped Diagnostic Context）
 * - logback 配置中通过 %X{traceId} 将其打印到每行日志，方便排查问题
 * - 同时将 traceId 写入响应头 X-Trace-Id，方便前端/客户端上报问题时携带
 *
 * 执行顺序：最先执行（Order = 1），确保所有日志都能包含 traceId
 */
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 优先使用前端传入的 traceId（方便全链路追踪），否则自动生成
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            // 写入 MDC，此后本线程所有日志都会包含该 traceId
            MDC.put(TRACE_ID_KEY, traceId);
            // 写入响应头，方便前端拿到 traceId 上报问题
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            chain.doFilter(request, response);
        } finally {
            // 请求结束后清理 MDC，防止线程池复用时数据污染
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
