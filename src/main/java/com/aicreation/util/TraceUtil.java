package com.aicreation.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪工具类
 *
 * @author AI-Creation Team
 * @since 1.0.0
 */
public class TraceUtil {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 生成链路ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 设置链路ID到MDC
     */
    public static String setTraceId() {
        String traceId = generateTraceId();
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 设置指定的链路ID到MDC
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前链路ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 清理MDC中的链路ID
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 在任务执行前设置链路ID，执行后清理
     */
    public static void executeWithTraceId(Runnable task) {
        String traceId = setTraceId();
        try {
            task.run();
        } finally {
            clearTraceId();
        }
    }

    /**
     * 在任务执行前设置链路ID，执行后清理，支持返回结果
     */
    public static <T> T executeWithTraceId(Callable<T> task) throws Exception {
        String traceId = setTraceId();
        try {
            return task.call();
        } finally {
            clearTraceId();
        }
    }

    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }
}