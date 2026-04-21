package com.aicreation.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMembershipSetEndReqDto {

    @NotNull
    private Long targetUserId;

    @NotNull
    private LocalDateTime newEndAt;

    private String remark;
}
