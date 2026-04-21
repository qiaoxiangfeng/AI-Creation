package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文章生成配置创建请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章生成配置创建请求")
public class ArticleGenerationConfigCreateReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章主题", example = "程序员逆袭")
    @NotBlank(message = "文章主题不能为空")
    private String theme;

    @Schema(description = "性别分类", example = "男生小说")
    @Size(max = 50, message = "性别分类不能超过50个字符")
    private String gender;

    @Schema(description = "题材分类", example = "玄幻")
    @Size(max = 100, message = "题材分类不能超过100个字符")
    private String genre;

    @Schema(description = "情节分类", example = "升级")
    @Size(max = 200, message = "情节分类不能超过200个字符")
    private String plot;

    @Schema(description = "角色分类", example = "主角光环")
    @Size(max = 100, message = "角色分类不能超过100个字符")
    private String characterType;

    @Schema(description = "风格分类", example = "热血")
    @Size(max = 100, message = "风格分类不能超过100个字符")
    private String style;

    @Schema(description = "附加特点", example = "搞笑,失落")
    @Size(max = 1000, message = "附加特点不能超过1000个字符")
    private String additionalCharacteristics;

    @Schema(description = "总字数预估", example = "100000")
    @Min(value = 1000, message = "总字数预估不能小于1000")
    private Integer totalWordCountEstimate;

    @Schema(description = "每章节字数预估", example = "5000")
    @Min(value = 500, message = "每章节字数预估不能小于500")
    private Integer chapterWordCountEstimate;

    @Schema(description = "待生成数量", example = "10")
    @Min(value = 0, message = "待生成数量不能小于0")
    private Integer pendingCount;

    @Schema(description = "创建人用户ID（前端传入当前登录用户）")
    private Long createUserId;

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

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
}