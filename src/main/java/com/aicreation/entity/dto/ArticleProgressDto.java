package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章生成进度DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Data
@Schema(description = "文章生成进度")
public class ArticleProgressDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章名称")
    private String articleName;

    @Schema(description = "总章节数")
    private Integer totalChapters;

    @Schema(description = "已完成章节数")
    private Integer completedChapters;

    @Schema(description = "进度百分比（0-100）")
    private Integer progressPercent;

    @Schema(description = "预估总字数")
    private Integer totalWordCountEstimate;

    @Schema(description = "当前已生成字数")
    private Integer currentWordCount;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "最后更新时间")
    private String updateTime;
}