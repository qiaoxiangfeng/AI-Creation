package com.aicreation.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipCreateOrderReqDto {

    @NotNull
    private Long pricingConfigId;

    /** ALIPAY / WECHAT */
    private String channel;
}
