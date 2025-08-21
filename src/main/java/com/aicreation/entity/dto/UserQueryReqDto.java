package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户查询请求")
public class UserQueryReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}



