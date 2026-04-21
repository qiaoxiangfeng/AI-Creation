package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体类
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 文章名称
     */
    private String articleName;

    /**
     * 文章简介
     */
    private String articleOutline;

    /**
     * 故事背景
     */
    private String storyBackground;


    /**
     * 形象描述
     */
    private String imageDesc;

    /**
     * 文章主题/分类（原 articleType）
     */
    private String theme;

    /**
     * 附加特点（生成配置的非主题字段值拼接，逗号分隔）
     */
    private String additionalCharacteristics;

    /**
     * 音色
     */
    private String voiceTone;

    /**
     * 语音链接
     */
    private String voiceLink;

    /**
     * 语音文件地址
     */
    private String voiceFilePath;

    /**
     * 视频链接
     */
    private String videoLink;

    /**
     * 视频文件地址
     */
    private String videoFilePath;

    /**
     * 发布状态：1-未发布，2-已发布
     */
    private Integer publishStatus;

    /**
     * 总字数预估
     */
    private Integer totalWordCountEstimate;

    /**
     * 每章节字数预估
     */
    private Integer chapterWordCountEstimate;

    /**
     * 章节完结标识：true-章节已完结，false-章节未完结
     */
    private Boolean storyComplete;

    /**
     * 删除标记：1-有效，0-无效
     */
    private Integer resState;

    /**
     * 创建人用户ID
     */
    private Long createUserId;

    /**
     * 创建人用户名（列表查询 JOIN 填充，非表字段）
     */
    private String createUserName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * Responses API的response_id，用于上下文管理
     */
    private String responseId;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getArticleOutline() {
        return articleOutline;
    }

    public void setArticleOutline(String articleOutline) {
        this.articleOutline = articleOutline;
    }

    public String getStoryBackground() {
        return storyBackground;
    }

    public void setStoryBackground(String storyBackground) {
        this.storyBackground = storyBackground;
    }


    public String getImageDesc() {
        return imageDesc;
    }

    public void setImageDesc(String imageDesc) {
        this.imageDesc = imageDesc;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getAdditionalCharacteristics() {
        return additionalCharacteristics;
    }

    public void setAdditionalCharacteristics(String additionalCharacteristics) {
        this.additionalCharacteristics = additionalCharacteristics;
    }


    public String getVoiceTone() {
        return voiceTone;
    }

    public void setVoiceTone(String voiceTone) {
        this.voiceTone = voiceTone;
    }

    public String getVoiceLink() {
        return voiceLink;
    }

    public void setVoiceLink(String voiceLink) {
        this.voiceLink = voiceLink;
    }

    public String getVoiceFilePath() {
        return voiceFilePath;
    }

    public void setVoiceFilePath(String voiceFilePath) {
        this.voiceFilePath = voiceFilePath;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public Integer getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Integer getTotalWordCountEstimate() {
        return totalWordCountEstimate;
    }

    public void setTotalWordCountEstimate(Integer totalWordCountEstimate) {
        this.totalWordCountEstimate = totalWordCountEstimate;
    }

    public Integer getChapterWordCountEstimate() {
        return chapterWordCountEstimate;
    }

    public void setChapterWordCountEstimate(Integer chapterWordCountEstimate) {
        this.chapterWordCountEstimate = chapterWordCountEstimate;
    }

    public Boolean getStoryComplete() {
        return storyComplete != null ? storyComplete : false;
    }

    public void setStoryComplete(Boolean storyComplete) {
        this.storyComplete = storyComplete;
    }

    public Integer getResState() {
        return resState;
    }

    public void setResState(Integer resState) {
        this.resState = resState;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }
}
