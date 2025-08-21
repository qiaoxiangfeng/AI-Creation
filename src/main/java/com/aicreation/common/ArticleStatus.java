package com.aicreation.common;

/**
 * 文章状态常量
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public class ArticleStatus {

    /**
     * 未发布
     */
    public static final Integer UNPUBLISHED = 1;

    /**
     * 已发布
     */
    public static final Integer PUBLISHED = 2;

    /**
     * 获取发布状态描述
     * 
     * @param status 状态值
     * @return 状态描述
     */
    public static String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "未发布";
            case 2:
                return "已发布";
            default:
                return "未知";
        }
    }

    /**
     * 判断是否为有效的发布状态
     * 
     * @param status 状态值
     * @return 是否有效
     */
    public static boolean isValidStatus(Integer status) {
        return status != null && (status == 1 || status == 2);
    }
}
