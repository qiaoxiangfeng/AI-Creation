package com.aicreation.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminMembershipGrantMonthsReqDto {

    @NotNull
    private Long targetUserId;

    @NotNull
    @Min(1)
    private Integer months;

    private String remark;
}
