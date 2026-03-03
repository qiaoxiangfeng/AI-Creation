package com.aicreation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 音频文件信息响应DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "音频文件信息响应")
public class TtsAudioInfoRespDto {
    
    @Schema(description = "请求ID")
    private String reqId;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件名")
    private String fileName;
    
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "最后修改时间")
    private Long lastModified;
    
    @Schema(description = "文件是否存在")
    private Boolean exists;
}
