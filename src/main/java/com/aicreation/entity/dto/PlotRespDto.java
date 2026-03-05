package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 伏笔响应DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Data
@Schema(description = "伏笔响应")
public class PlotRespDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "伏笔ID")
    private Long id;

    @Schema(description = "伏笔名称")
    private String plotName;

    @Schema(description = "伏笔内容")
    private String plotContent;

    @Schema(description = "回收章节ID")
    private Long recoveryChapterId;

    @Schema(description = "回收章节序号")
    private Integer recoveryChapterNo;
}