package com.aicreation.enums;

/**
 * 音色类型枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum VoiceTypeEnum {
    
    /**
     * 中文女声-灿灿
     */
    ZH_FEMALE_CANCAN_MARS_BIGTTS("zh_female_cancan_mars_bigtts", "中文女声-灿灿"),
    
    /**
     * 中文男声-M392
     */
    ZH_MALE_M392_CONVERSATION_WVAE_BIGTTS("zh_male_M392_conversation_wvae_bigtts", "中文男声-M392"),
    
    /**
     * 中文女声-小美
     */
    ZH_FEMALE_XIAOMEI_BIGTTS("zh_female_xiaomei_bigtts", "中文女声-小美"),
    
    /**
     * 中文男声-小明
     */
    ZH_MALE_XIAOMING_BIGTTS("zh_male_xiaoming_bigtts", "中文男声-小明"),
    
    /**
     * 英文女声-Emma
     */
    EN_FEMALE_EMMA_BIGTTS("en_female_emma_bigtts", "英文女声-Emma"),
    
    /**
     * 英文男声-James
     */
    EN_MALE_JAMES_BIGTTS("en_male_james_bigtts", "英文男声-James");
    
    private final String code;
    private final String description;
    
    VoiceTypeEnum(String code, String description) {
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
    public static VoiceTypeEnum fromCode(String code) {
        for (VoiceTypeEnum voiceType : values()) {
            if (voiceType.getCode().equals(code)) {
                return voiceType;
            }
        }
        throw new IllegalArgumentException("未知的音色类型: " + code);
    }
    
    /**
     * 获取默认音色
     */
    public static VoiceTypeEnum getDefault() {
        return ZH_FEMALE_CANCAN_MARS_BIGTTS;
    }
}
