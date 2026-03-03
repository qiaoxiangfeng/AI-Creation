package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文章生成配置更新请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章生成配置更新请求")
public class ArticleGenerationConfigUpdateReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID", example = "1")
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "文章主题", example = "程序员逆袭")
    @Size(max = 100, message = "文章主题不能超过100个字符")
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

    @Schema(description = "待生成数量", example = "10")
    @Min(value = 0, message = "待生成数量不能小于0")
    private Integer pendingCount;

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

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }
}