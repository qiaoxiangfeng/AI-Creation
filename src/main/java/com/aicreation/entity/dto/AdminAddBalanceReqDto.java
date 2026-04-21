package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理员手动补入用户余额请求
 *
 * 金额单位：分（Cent）
 */
@Getter
@Setter
@Schema(description = "管理员手动补入用户余额请求")
public class AdminAddBalanceReqDto {

    @Min(1)
    @Schema(description = "补入金额（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amountCent;

    @Schema(description = "备注（用于流水说明/对用户通知）")
    private String remark;

    @Schema(description = "幂等键（可选），用于避免重复补入")
    private String idempotencyKey;
}

