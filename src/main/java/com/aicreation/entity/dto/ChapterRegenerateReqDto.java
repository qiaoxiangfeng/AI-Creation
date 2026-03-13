package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 章节内容重新生成请求 DTO
 *
 * @author AI-Creation Team
 * @date 2026/03/12
 * @version 1.0.0
 */
@Data
@Schema(description = "章节内容重新生成请求")
public class ChapterRegenerateReqDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户对本章节内容的修改意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修改意见不能为空")
    private String instruction;
}

