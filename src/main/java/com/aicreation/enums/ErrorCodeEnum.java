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
     * 无权限访问（未细分场景时的通用提示）
     */
    NO_PERMISSION("CM06", "无权限访问"),

    /**
     * 非管理员无法修改或删除全局字典
     */
    GLOBAL_DICTIONARY_WRITE_DENIED("CM11", "非管理员用户无法修改或删除全局字典"),

    /**
     * 非资源创建者（文章 / 生成配置 / 个人字典等）
     */
    RESOURCE_NOT_OWNED("CM12", "无权操作他人创建的内容"),

    /**
     * 个人字典仅创建人可读
     */
    DICTIONARY_READ_DENIED("CM13", "无权查看他人创建的字典"),

    /**
     * 需要登录
     */
    LOGIN_REQUIRED("CM14", "请先登录后再操作"),

    /**
     * 仅管理员可操作
     */
    ADMIN_ONLY("CM15", "仅管理员可执行此操作"),
    
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
    DUPLICATE_DATA("CM10", "数据重复"),

    /**
     * 余额不足
     */
    BALANCE_INSUFFICIENT("CM16", "余额不足，无法使用AI功能"),

    /**
     * 充值订单不存在
     */
    RECHARGE_ORDER_NOT_FOUND("CM17", "充值订单不存在"),

    /**
     * 支付回调验签失败
     */
    PAY_CALLBACK_INVALID("CM18", "支付回调验签失败"),

    /**
     * 支付金额不匹配
     */
    PAY_AMOUNT_MISMATCH("CM19", "支付金额不匹配"),

    /**
     * 计费预占失败
     */
    BILLING_PREAUTH_FAILED("CM20", "计费预占失败"),

    /**
     * 计费结算失败
     */
    BILLING_SETTLEMENT_FAILED("CM21", "计费结算失败"),

    /**
     * 需要开通会员才可使用 AI
     */
    MEMBERSHIP_REQUIRED("CM22", "请先开通会员后使用 AI 功能"),

    /**
     * 会员已过期
     */
    MEMBERSHIP_EXPIRED("CM23", "会员已过期，请续费后使用 AI 功能"),

    /**
     * 会员定价配置不存在或已下架
     */
    MEMBERSHIP_PRICING_NOT_FOUND("CM24", "会员套餐不存在或已下架");
    
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
