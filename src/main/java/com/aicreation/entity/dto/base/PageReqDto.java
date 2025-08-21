package com.aicreation.entity.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * 分页通用请求对象
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "分页通用请求参数")
public class PageReqDto extends BaseDto {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "每页记录数", example = "20", defaultValue = "20")
    private Integer pageSize = 20;

    /**
     * 获取验证后的页码
     * 如果页码为空或小于1，则返回1
     * 
     * @return 验证后的页码
     */
    public Integer getValidatedPageNo() {
        if (Objects.isNull(pageNo) || pageNo < 1) {
            return 1;
        }
        return pageNo;
    }

    /**
     * 获取验证后的每页记录数
     * 如果每页记录数为空、小于1或大于100，则返回20
     * 
     * @return 验证后的每页记录数
     */
    public Integer getValidatedPageSize() {
        if (Objects.isNull(pageSize) || pageSize < 1 || pageSize > 100) {
            return 20;
        }
        return pageSize;
    }

    /**
     * 验证分页参数是否有效
     * 
     * @return 如果参数有效返回true，否则返回false
     */
    public boolean isValid() {
        return Objects.nonNull(pageNo) && pageNo >= 1 
            && Objects.nonNull(pageSize) && pageSize >= 1 && pageSize <= 100;
    }

    // Getter and Setter methods
    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
