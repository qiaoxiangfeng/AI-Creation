package com.aicreation.exception;

/**
 * 文件不存在异常
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public class FileNotFoundException extends RuntimeException {
    
    public FileNotFoundException(String message) {
        super(message);
    }
    
    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
