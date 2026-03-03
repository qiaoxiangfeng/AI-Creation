package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典创建请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "字典创建请求")
public class DictionaryCreateReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典键", example = "文章特点")
    @NotBlank(message = "字典键不能为空")
    @Size(max = 100, message = "字典键不能超过100个字符")
    private String dictKey;

    @Schema(description = "字典值", example = "搞笑")
    @NotBlank(message = "字典值不能为空")
    @Size(max = 500, message = "字典值不能超过500个字符")
    private String dictValue;

    @Schema(description = "排序顺序", example = "1")
    @Min(value = 0, message = "排序顺序不能小于0")
    private Integer sortOrder;

    // Getter and Setter methods
    public String getDictKey() {
        return dictKey;
    }

    public void setDictKey(String dictKey) {
        this.dictKey = dictKey;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}