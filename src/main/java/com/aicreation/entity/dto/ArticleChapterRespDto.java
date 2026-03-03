package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章章节响应DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Data
@Schema(description = "文章章节响应")
public class ArticleChapterRespDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "章节ID")
    private Long id;

    @Schema(description = "章节序号")
    private Integer chapterNo;

    @Schema(description = "章节标题")
    private String chapterTitle;

    @Schema(description = "章节内容")
    private String chapterContent;

    @Schema(description = "核心剧情")
    private String corePlot;

    @Schema(description = "字数预估")
    private Integer wordCountEstimate;

    @Schema(description = "章节语音链接")
    private String chapterVoiceLink;

    @Schema(description = "章节视频链接")
    private String chapterVideoLink;
}