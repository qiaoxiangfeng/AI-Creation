package com.aicreation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 异步语音合成响应DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "异步语音合成响应")
public class TtsAsyncRespDto {
    
    @Schema(description = "任务ID")
    private String taskId;
    
    @Schema(description = "任务状态")
    private String status;
    
    @Schema(description = "响应消息")
    private String message;
}
