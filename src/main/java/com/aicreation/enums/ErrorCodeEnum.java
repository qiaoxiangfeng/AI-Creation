package com.aicreation.enums;

/**
 * 统一错误码枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum ErrorCodeEnum {
    
    /**
     * 请求正确
     */
    SUCCESS("3000", "请求正确"),
    
    /**
     * 无效的请求
     */
    INVALID_REQUEST("3001", "无效的请求"),
    
    /**
     * 并发超限
     */
    CONCURRENCY_LIMIT("3003", "并发超限"),
    
    /**
     * 后端服务忙
     */
    SERVICE_BUSY("3005", "后端服务忙"),
    
    /**
     * 服务中断
     */
    SERVICE_INTERRUPTED("3006", "服务中断"),
    
    /**
     * 文本长度超限
     */
    TEXT_TOO_LONG("3010", "文本长度超限"),
    
    /**
     * 无效文本
     */
    INVALID_TEXT("3011", "无效文本"),
    
    /**
     * 处理超时
     */
    PROCESSING_TIMEOUT("3030", "处理超时"),
    
    /**
     * 处理错误
     */
    PROCESSING_ERROR("3031", "处理错误"),
    
    /**
     * 等待获取音频超时
     */
    AUDIO_TIMEOUT("3032", "等待获取音频超时"),
    
    /**
     * 后端链路连接错误
     */
    BACKEND_CONNECTION_ERROR("3040", "后端链路连接错误"),
    
    /**
     * 音色不存在
     */
    VOICE_NOT_FOUND("3050", "音色不存在"),
    
    /**
     * 文件不存在
     */
    FILE_NOT_FOUND("4040", "文件不存在"),
    
    /**
     * 参数验证失败
     */
    PARAM_VALIDATION_ERROR("4001", "参数验证失败"),
    
    /**
     * 参数绑定失败
     */
    PARAM_BIND_ERROR("4002", "参数绑定失败"),
    
    /**
     * 约束验证失败
     */
    CONSTRAINT_VIOLATION_ERROR("4003", "约束验证失败"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR("4004", "参数错误"),
    
    /**
     * 系统内部错误
     */
    INTERNAL_ERROR("5000", "系统内部错误"),
    
    /**
     * 系统异常
     */
    SYSTEM_ERROR("5999", "系统异常"),
    
    // ========== 通用业务错误码 ==========
    
    /**
     * 成功
     */
    COMMON_SUCCESS("00000000", "success"),
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND("CM03", "用户不存在"),
    
    /**
     * 用户名或密码错误
     */
    USERNAME_OR_PASSWORD_ERROR("CM04", "用户名或密码错误"),
    
    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS("CM05", "用户已存在"),
    
    /**
     * 无权限访问
     */
    NO_PERMISSION("CM06", "无权限访问"),
    
    /**
     * Token无效
     */
    TOKEN_INVALID("CM07", "Token无效"),
    
    /**
     * Token过期
     */
    TOKEN_EXPIRED("CM08", "Token过期"),
    
    /**
     * 数据不存在
     */
    DATA_NOT_FOUND("CM09", "数据不存在"),
    
    /**
     * 数据重复
     */
    DUPLICATE_DATA("CM10", "数据重复");
    
    private final String code;
    private final String message;
    
    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static ErrorCodeEnum fromCode(String code) {
        for (ErrorCodeEnum errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("未知的错误码: " + code);
    }
}
