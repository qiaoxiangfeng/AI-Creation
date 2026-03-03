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
     * 文章类型
     */
    private String articleType;

    /**
     * 文章特点
     */
    private String articleCharacteristics;

    /**
     * 文章内容
     */
    private String articleContent;

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
     * 内容生成状态：0-未生成，1-已生成
     */
    private Integer contentGenerated;

    /**
     * 总字数预估
     */
    private Integer totalWordCountEstimate;

    /**
     * 每章节字数预估
     */
    private Integer chapterWordCountEstimate;

    /**
     * 生成状态：0-未开始，1-生成中，2-已完成，3-失败
     */
    private Integer generationStatus;

    /**
     * 删除标记：1-有效，0-无效
     */
    private Integer resState;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

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

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public String getArticleCharacteristics() {
        return articleCharacteristics;
    }

    public void setArticleCharacteristics(String articleCharacteristics) {
        this.articleCharacteristics = articleCharacteristics;
    }

    public String getArticleContent() {
        return articleContent;
    }

    public void setArticleContent(String articleContent) {
        this.articleContent = articleContent;
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

    public Integer getContentGenerated() {
        return contentGenerated;
    }

    public void setContentGenerated(Integer contentGenerated) {
        this.contentGenerated = contentGenerated;
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

    public Integer getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(Integer generationStatus) {
        this.generationStatus = generationStatus;
    }

    public Integer getResState() {
        return resState;
    }

    public void setResState(Integer resState) {
        this.resState = resState;
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
}
