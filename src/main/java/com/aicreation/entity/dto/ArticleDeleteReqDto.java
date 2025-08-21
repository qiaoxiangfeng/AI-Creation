package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

/**
 * 文章删除请求DTO
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章删除请求")
public class ArticleDeleteReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID", example = "1")
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }
}
