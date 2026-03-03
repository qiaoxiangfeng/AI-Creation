package com.aicreation.enums;

/**
 * 音色枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum VoiceToneEnum {
    
    /**
     * Alex音色
     */
    ALEX("alex", "Alex"),
    
    /**
     * Anna音色
     */
    ANNA("anna", "Anna");
    
    private final String code;
    private final String description;
    
    VoiceToneEnum(String code, String description) {
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
    public static VoiceToneEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (VoiceToneEnum voiceTone : values()) {
            if (voiceTone.getCode().equals(code)) {
                return voiceTone;
            }
        }
        throw new IllegalArgumentException("未知的音色: " + code);
    }
    
    /**
     * 判断是否为有效的音色值
     * 
     * @param code 音色值
     * @return 是否有效
     */
    public static boolean isValidVoiceTone(String code) {
        if (code == null) {
            return false;
        }
        for (VoiceToneEnum voiceTone : values()) {
            if (voiceTone.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取所有可用的音色值
     * 
     * @return 音色值数组
     */
    public static String[] getAllVoiceTones() {
        return new String[]{ALEX.getCode(), ANNA.getCode()};
    }
}
