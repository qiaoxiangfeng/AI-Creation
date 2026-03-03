package com.aicreation.enums;

/**
 * 语音合成类型枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum SynthesisTypeEnum {
    
    /**
     * HTTP方式合成
     */
    HTTP("HTTP", "HTTP方式合成"),
    
    /**
     * 异步合成
     */
    ASYNC("ASYNC", "异步合成"),
    
    /**
     * 流式合成
     */
    STREAM("STREAM", "流式合成");
    
    private final String code;
    private final String description;
    
    SynthesisTypeEnum(String code, String description) {
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
    public static SynthesisTypeEnum fromCode(String code) {
        for (SynthesisTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的合成类型: " + code);
    }
}
