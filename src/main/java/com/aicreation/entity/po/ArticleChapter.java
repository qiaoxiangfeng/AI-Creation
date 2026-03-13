package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章章节实体
 */
public class ArticleChapter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 章节序号（第一章填1） */
    private Integer chapterNo;

    /** 文章ID，关联 article.id */
    private Long articleId;

    /** 章节标题 */
    private String chapterTitle;

    /** 章节内容 */
    private String chapterContent;

    /** 核心剧情 */
    private String corePlot;

    /** 字数预估 */
    private Integer wordCountEstimate;

    /** 生成本章章节信息时的 response_id，用于本章内容生成的上下文起点 */
    private String responseIdPlan;

    /** 生成本章正文内容时的 response_id，用于后续重新生成的上下文起点 */
    private String responseIdContent;

    /** 章节语音链接地址 */
    private String chapterVoiceLink;

    /** 章节视频链接地址 */
    private String chapterVideoLink;

    /** 删除标记：1-有效，0-无效 */
    private Integer resState;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 生成状态（0-未开始，1-生成中，2-已完成，3-失败） */
    private Integer generationStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo) {
        this.chapterNo = chapterNo;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getChapterContent() {
        return chapterContent;
    }

    public void setChapterContent(String chapterContent) {
        this.chapterContent = chapterContent;
    }

    public String getCorePlot() {
        return corePlot;
    }

    public void setCorePlot(String corePlot) {
        this.corePlot = corePlot;
    }

    public Integer getWordCountEstimate() {
        return wordCountEstimate;
    }

    public void setWordCountEstimate(Integer wordCountEstimate) {
        this.wordCountEstimate = wordCountEstimate;
    }

    public String getResponseIdPlan() {
        return responseIdPlan;
    }

    public void setResponseIdPlan(String responseIdPlan) {
        this.responseIdPlan = responseIdPlan;
    }

    public String getResponseIdContent() {
        return responseIdContent;
    }

    public void setResponseIdContent(String responseIdContent) {
        this.responseIdContent = responseIdContent;
    }

    public String getChapterVoiceLink() {
        return chapterVoiceLink;
    }

    public void setChapterVoiceLink(String chapterVoiceLink) {
        this.chapterVoiceLink = chapterVoiceLink;
    }

    public String getChapterVideoLink() {
        return chapterVideoLink;
    }

    public void setChapterVideoLink(String chapterVideoLink) {
        this.chapterVideoLink = chapterVideoLink;
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

    public Integer getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(Integer generationStatus) {
        this.generationStatus = generationStatus;
    }
}


