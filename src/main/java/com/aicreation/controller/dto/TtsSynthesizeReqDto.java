package com.aicreation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 语音合成请求DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "语音合成请求")
public class TtsSynthesizeReqDto {
    
    @NotBlank(message = "要合成的文本不能为空")
    @Size(max = 1024, message = "文本长度不能超过1024个字符")
    @Schema(description = "要合成的文本", example = "你好，我是豆包语音助手")
    private String text;
    
    @Schema(description = "音色类型", example = "zh_female_cancan_mars_bigtts")
    private String voiceType;
    
    @Schema(description = "是否保存文件", example = "true")
    private Boolean saveFile = true;
}
