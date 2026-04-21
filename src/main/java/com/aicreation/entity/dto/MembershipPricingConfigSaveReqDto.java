package com.aicreation.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MembershipPricingConfigSaveReqDto {

    private Long id;

    @NotBlank
    private String tier;

    @NotNull
    private Integer durationMonths;

    @NotNull
    private Long baseMonthPriceCent;

    @NotNull
    private BigDecimal discountRate;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Integer sortOrder;
}
