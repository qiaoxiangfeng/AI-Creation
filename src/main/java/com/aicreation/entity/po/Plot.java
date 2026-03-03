package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 伏笔实体
 */
public class Plot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 埋设伏笔的章节ID */
    private Long chapterId;

    /** 伏笔名称 */
    private String plotName;

    /** 伏笔内容 */
    private String plotContent;

    /** 回收伏笔的章节ID */
    private Long recoveryChapterId;

    /** 删除标记：1-有效，0-无效 */
    private Integer resState;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getPlotName() {
        return plotName;
    }

    public void setPlotName(String plotName) {
        this.plotName = plotName;
    }

    public String getPlotContent() {
        return plotContent;
    }

    public void setPlotContent(String plotContent) {
        this.plotContent = plotContent;
    }

    public Long getRecoveryChapterId() {
        return recoveryChapterId;
    }

    public void setRecoveryChapterId(Long recoveryChapterId) {
        this.recoveryChapterId = recoveryChapterId;
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