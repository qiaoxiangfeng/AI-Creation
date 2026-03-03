package com.aicreation.enums;

/**
 * 音频文件类型枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum AudioFileTypeEnum {
    
    /**
     * MP3格式
     */
    MP3("mp3", "MP3格式"),
    
    /**
     * WAV格式
     */
    WAV("wav", "WAV格式"),
    
    /**
     * PCM格式
     */
    PCM("pcm", "PCM格式"),
    
    /**
     * OPUS格式
     */
    OPUS("opus", "OPUS格式");
    
    private final String code;
    private final String description;
    
    AudioFileTypeEnum(String code, String description) {
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
    public static AudioFileTypeEnum fromCode(String code) {
        for (AudioFileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的音频文件类型: " + code);
    }
    
    /**
     * 根据编码格式获取文件扩展名
     */
    public static String getFileExtension(String encoding) {
        if (encoding == null) {
            return MP3.getCode();
        }

        switch (encoding.toLowerCase()) {
            case "mp3":
                return MP3.getCode();
            case "wav":
                return WAV.getCode();
            case "pcm":
                return PCM.getCode();
            case "ogg_opus":
                return OPUS.getCode();
            default:
                return MP3.getCode();
        }
    }
}
