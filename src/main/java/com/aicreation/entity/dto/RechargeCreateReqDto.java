package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "创建充值订单请求")
public class RechargeCreateReqDto {

    @NotNull
    @Min(1)
    @Schema(description = "充值金额（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amountCent;

    @Schema(description = "渠道（ALIPAY/WECHAT），当前优先支持 ALIPAY")
    private String channel;
}

