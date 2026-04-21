package com.aicreation.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "钱包余额响应")
public class WalletBalanceRespDto {
    @Schema(description = "总余额（分）")
    private Long totalBalanceCent;

    @Schema(description = "冻结余额（分）")
    private Long frozenBalanceCent;

    @Schema(description = "可用余额（分）")
    private Long availableBalanceCent;
}

