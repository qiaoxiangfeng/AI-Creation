package com.aicreation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 异步语音合成请求DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "异步语音合成请求")
public class TtsAsyncReqDto {
    
    @NotBlank(message = "要合成的文本不能为空")
    @Size(max = 1024, message = "文本长度不能超过1024个字符")
    @Schema(description = "要合成的文本", example = "这是异步语音合成测试")
    private String text;
    
    @Schema(description = "音色类型", example = "zh_female_cancan_mars_bigtts")
    private String voiceType;
}
