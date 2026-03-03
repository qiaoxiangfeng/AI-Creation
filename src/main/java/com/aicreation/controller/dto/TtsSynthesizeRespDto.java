package com.aicreation.controller.dto;

import com.aicreation.external.dto.TtsResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 语音合成响应DTO
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
@Schema(description = "语音合成响应")
public class TtsSynthesizeRespDto {
    
    @Schema(description = "请求ID")
    private String reqId;
    
    @Schema(description = "状态码")
    private Integer code;
    
    @Schema(description = "响应消息")
    private String message;
    
    @Schema(description = "音频数据（Base64编码）")
    private String audioData;
    
    @Schema(description = "音频时长（毫秒）")
    private String duration;
    
    @Schema(description = "文件路径（如果保存了文件）")
    private String filePath;
    
    /**
     * 从TtsResponse构建响应DTO
     */
    public static TtsSynthesizeRespDto fromTtsResponse(TtsResponse response, String filePath) {
        TtsSynthesizeRespDto dto = new TtsSynthesizeRespDto();
        dto.setReqId(response.getReqId());
        dto.setCode(response.getCode());
        dto.setMessage(response.getMessage());
        dto.setAudioData(response.getData());
        dto.setFilePath(filePath);
        
        if (response.getAddition() != null) {
            dto.setDuration(response.getAddition().getDuration());
        }
        
        return dto;
    }
}
