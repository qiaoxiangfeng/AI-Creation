package com.aicreation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 删除音频文件响应DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "删除音频文件响应")
public class TtsDeleteRespDto {
    
    @Schema(description = "请求ID")
    private String reqId;
    
    @Schema(description = "删除结果")
    private Boolean success;
    
    @Schema(description = "响应消息")
    private String message;
}
