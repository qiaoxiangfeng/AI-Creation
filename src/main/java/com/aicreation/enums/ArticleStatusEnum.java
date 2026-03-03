package com.aicreation.enums;

/**
 * 文章状态枚举
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public enum ArticleStatusEnum {
    
    /**
     * 未发布
     */
    UNPUBLISHED(1, "未发布"),
    
    /**
     * 已发布
     */
    PUBLISHED(2, "已发布");
    
    private final Integer code;
    private final String description;
    
    ArticleStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static ArticleStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ArticleStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的文章状态: " + code);
    }
    
    /**
     * 判断是否为有效的发布状态
     * 
     * @param code 状态值
     * @return 是否有效
     */
    public static boolean isValidStatus(Integer code) {
        if (code == null) {
            return false;
        }
        for (ArticleStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
