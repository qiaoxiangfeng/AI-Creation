package com.aicreation.common;

/**
 * 错误码枚举
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS("00000000", "success"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR("CM01", "系统异常"),

    /**
     * 参数错误
     */
    PARAM_ERROR("CM02", "参数错误"),

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

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误描述
     */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
} 