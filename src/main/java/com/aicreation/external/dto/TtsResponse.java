package com.aicreation.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 豆包语音合成响应模型
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
public class TtsResponse {
    
    @JsonProperty("reqid")
    private String reqId;
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("sequence")
    private Integer sequence;
    
    @JsonProperty("data")
    private String data;
    
    @JsonProperty("addition")
    private Addition addition;
    
    @Data
    public static class Addition {
        @JsonProperty("duration")
        private String duration;
    }
}
