package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "创建充值订单响应")
public class RechargeCreateRespDto {

    @Schema(description = "商户订单号")
    private String orderNo;

    @Schema(description = "支付二维码内容/URL（前端生成二维码展示）")
    private String payUrl;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}

