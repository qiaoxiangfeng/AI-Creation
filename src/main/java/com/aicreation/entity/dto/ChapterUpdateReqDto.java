package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 章节更新请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Data
@Schema(description = "章节更新请求")
public class ChapterUpdateReqDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "核心剧情")
    private String corePlot;

    @Schema(description = "字数预估")
    private Integer wordCountEstimate;

    @Schema(description = "伏笔列表")
    @Valid
    private List<PlotReqDto> plots;
}