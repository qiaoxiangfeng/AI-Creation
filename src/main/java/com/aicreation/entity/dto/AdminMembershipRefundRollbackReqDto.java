package com.aicreation.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminMembershipRefundRollbackReqDto {

    @NotNull
    private Long paymentOrderId;

    private String remark;
}
