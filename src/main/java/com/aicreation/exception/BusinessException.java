package com.aicreation.exception;

import com.aicreation.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原因异常
     */
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码枚举
     * @param message 错误消息
     */
    public BusinessException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
