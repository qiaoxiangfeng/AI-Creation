package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 文章生成配置列表响应DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章生成配置列表响应")
public class ArticleGenerationConfigListRespDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "文章主题", example = "程序员逆袭")
    private String theme;

    @Schema(description = "性别分类", example = "男生小说")
    private String gender;

    @Schema(description = "题材分类", example = "玄幻")
    private String genre;

    @Schema(description = "情节分类", example = "升级")
    private String plot;

    @Schema(description = "角色分类", example = "主角光环")
    private String characterType;

    @Schema(description = "风格分类", example = "热血")
    private String style;

    @Schema(description = "附加特点", example = "搞笑,失落")
    private String additionalCharacteristics;

    @Schema(description = "总字数预估", example = "100000")
    private Integer totalWordCountEstimate;

    @Schema(description = "每章节字数预估", example = "5000")
    private Integer chapterWordCountEstimate;

    @Schema(description = "待生成数量", example = "10")
    private Integer pendingCount;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    private LocalDateTime createTime;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}