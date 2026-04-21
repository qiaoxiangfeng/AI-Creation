package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文章大纲 AI 修改请求DTO
 */
@Schema(description = "文章大纲AI修改请求")
public class ArticleOutlineRefineReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "修改意见", example = "请加强开篇冲突，压缩日常描写，突出主角目标，并把大纲分为三幕结构。")
    @NotBlank(message = "修改意见不能为空")
    @Size(max = 2000, message = "修改意见不能超过2000个字符")
    private String instruction;

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}

