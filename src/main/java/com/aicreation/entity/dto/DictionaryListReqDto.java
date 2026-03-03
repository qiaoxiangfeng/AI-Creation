package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.PageReqDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典列表查询请求DTO
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "字典列表查询请求")
public class DictionaryListReqDto extends PageReqDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典键", example = "文章特点")
    private String dictKey;

    @Schema(description = "字典值", example = "搞笑")
    private String dictValue;

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
}