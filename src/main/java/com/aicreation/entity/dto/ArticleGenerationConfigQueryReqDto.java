package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

/**
 * 文章生成配置查询请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章生成配置查询请求")
public class ArticleGenerationConfigQueryReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID", example = "1")
    @NotNull(message = "ID不能为空")
    private Long id;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}