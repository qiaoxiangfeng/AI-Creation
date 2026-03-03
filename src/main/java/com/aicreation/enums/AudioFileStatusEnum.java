package com.aicreation.enums;

/**
 * 音频文件状态枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum AudioFileStatusEnum {
    
    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),
    
    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),
    
    /**
     * 失败
     */
    FAILED("FAILED", "失败");
    
    private final String code;
    private final String description;
    
    AudioFileStatusEnum(String code, String description) {
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
    public static AudioFileStatusEnum fromCode(String code) {
        for (AudioFileStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的音频文件状态: " + code);
    }
}
