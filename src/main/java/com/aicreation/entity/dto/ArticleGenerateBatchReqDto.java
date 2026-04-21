package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 文章按需批量生成请求
 */
@Schema(description = "文章按需批量生成请求（章节/章节内容）")
public class ArticleGenerateBatchReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "生成数量（不传则按 all=true 或默认逻辑处理）", example = "10")
    @Min(value = 1, message = "生成数量不能小于1")
    @Max(value = 1000, message = "生成数量不能大于1000")
    private Integer count;

    @Schema(description = "是否生成全部（true 时忽略 count）", example = "true")
    private Boolean all;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getAll() {
        return all;
    }

    public void setAll(Boolean all) {
        this.all = all;
    }
}

