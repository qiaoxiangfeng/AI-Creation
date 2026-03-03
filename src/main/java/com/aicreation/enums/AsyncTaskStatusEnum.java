package com.aicreation.enums;

/**
 * 异步任务状态枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum AsyncTaskStatusEnum {
    
    /**
     * 已提交
     */
    SUBMITTED("SUBMITTED", "已提交"),
    
    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),
    
    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),
    
    /**
     * 失败
     */
    FAILED("FAILED", "失败"),
    
    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消");
    
    private final String code;
    private final String description;
    
    AsyncTaskStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static AsyncTaskStatusEnum fromCode(String code) {
        for (AsyncTaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的异步任务状态: " + code);
    }
}
