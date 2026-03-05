package com.aicreation.external.config;

/**
 * AI调用统一配置
 * 集中管理AI调用的地址、模型等配置信息
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
public class AIConfig {

    /**
     * Volcengine API基础URL
     */
    public static final String VOLCENGINE_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    /**
     * 推理模型 - 用于复杂推理任务
     */
    public static final String MODEL_REASONING = "doubao-seed-1-6-lite-251015";

    /**
     * 专业模型 - 用于标准文本生成任务
     */
    public static final String MODEL_PROFESSIONAL = "doubao-pro-32k-241215";

    /**
     * 默认使用的模型
     */
    public static final String DEFAULT_MODEL = MODEL_REASONING;

    /**
     * 根据任务类型获取推荐的模型
     *
     * @param taskType 任务类型：title, content, reasoning等
     * @return 推荐的模型ID
     */
    public static String getRecommendedModel(String taskType) {
        switch (taskType.toLowerCase()) {
            case "title":
            case "content":
                return MODEL_REASONING; // 标题和大纲需要推理能力
            case "chat":
            case "simple":
                return MODEL_PROFESSIONAL; // 简单对话使用专业模型
            default:
                return DEFAULT_MODEL;
        }
    }

    /**
     * 获取API基础URL
     */
    public static String getBaseUrl() {
        return VOLCENGINE_BASE_URL;
    }
}