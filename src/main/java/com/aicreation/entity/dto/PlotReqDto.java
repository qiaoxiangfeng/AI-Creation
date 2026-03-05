package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * 伏笔请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Data
@Schema(description = "伏笔请求")
public class PlotReqDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "伏笔ID（更新时必填）")
    private Long id;

    @Schema(description = "伏笔名称", required = true)
    @NotBlank(message = "伏笔名称不能为空")
    private String plotName;

    @Schema(description = "伏笔内容", required = true)
    @NotBlank(message = "伏笔内容不能为空")
    private String plotContent;

    @Schema(description = "回收章节ID")
    private Long recoveryChapterId;
}