package com.aicreation.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 链路ID拦截器
 * 为每个请求生成唯一的链路ID，用于日志追踪
 *
 * @author AI-Creation Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 生成链路ID
        String traceId = generateTraceId();

        // 将链路ID放入MDC，供日志使用
        MDC.put(TRACE_ID_KEY, traceId);

        // 将链路ID添加到响应头，方便客户端查看
        response.setHeader("X-Trace-Id", traceId);

        log.info("开始处理请求: {} {}", request.getMethod(), request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清理MDC
        String traceId = MDC.get(TRACE_ID_KEY);
        log.info("完成处理请求: {} {}", request.getMethod(), request.getRequestURI());
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 生成链路ID
     * 使用UUID的前8位作为链路ID，既保证唯一性又保持简洁
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}