package com.aicreation.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MembershipPricingItemRespDto {

    private Long id;
    private String tier;
    private Integer durationMonths;
    private Long baseMonthPriceCent;
    private BigDecimal discountRate;
    /** 应付金额（分） */
    private Long priceCent;
}
