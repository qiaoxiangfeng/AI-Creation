package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "消息已读请求")
public class NotificationReadReqDto {
    @NotNull
    @Schema(description = "消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}

