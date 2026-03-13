package com.aicreation.entity.bo;

import java.time.LocalDateTime;

/**
 * 文章生成配置业务对象
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public class ArticleGenerationConfigBo {

    /**
     * 文章主题（用户自定义输入）
     */
    private String theme;

    /**
     * 性别分类（男生小说、女生小说）
     */
    private String gender;

    /**
     * 题材分类（仙侠、玄幻、都市等）
     */
    private String genre;

    /**
     * 情节分类（升级、学院、人生赢家等）
     */
    private String plot;

    /**
     * 角色分类
     */
    private String characterType;

    /**
     * 风格分类
     */
    private String style;

    /**
     * 附加特点
     */
    private String additionalCharacteristics;

    /**
     * 总字数预估
     */
    private Integer totalWordCountEstimate;

    /**
     * 每章节字数预估
     */
    private Integer chapterWordCountEstimate;

    /**
     * 待生成数量
     */
    private Integer pendingCount;

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
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getCharacterType() {
        return characterType;
    }

    public void setCharacterType(String characterType) {
        this.characterType = characterType;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getAdditionalCharacteristics() {
        return additionalCharacteristics;
    }

    public void setAdditionalCharacteristics(String additionalCharacteristics) {
        this.additionalCharacteristics = additionalCharacteristics;
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

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
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