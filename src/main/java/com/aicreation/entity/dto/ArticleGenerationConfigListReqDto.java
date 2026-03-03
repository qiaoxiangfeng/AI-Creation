package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.PageReqDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文章生成配置列表查询请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章生成配置列表查询请求")
public class ArticleGenerationConfigListReqDto extends PageReqDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章主题", example = "程序员逆袭")
    private String theme;

    @Schema(description = "性别分类", example = "男生小说")
    private String gender;

    @Schema(description = "题材分类", example = "玄幻")
    private String genre;

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
}