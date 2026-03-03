package com.aicreation.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 豆包语音合成请求模型
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
public class TtsRequest {
    
    @JsonProperty("app")
    private App app;
    
    @JsonProperty("user")
    private User user;
    
    @JsonProperty("audio")
    private Audio audio;
    
    @JsonProperty("request")
    private Request request;
    
    @Data
    public static class App {
        @JsonProperty("appid")
        private String appId;
        
        @JsonProperty("token")
        private String token;
        
        @JsonProperty("cluster")
        private String cluster;
    }
    
    @Data
    public static class User {
        @JsonProperty("uid")
        private String uid;
    }
    
    @Data
    public static class Audio {
        @JsonProperty("voice_type")
        private String voiceType;
        
        @JsonProperty("encoding")
        private String encoding;
        
        @JsonProperty("speed_ratio")
        private Double speedRatio;
        
        @JsonProperty("rate")
        private Integer rate;
        
        @JsonProperty("bitrate")
        private Integer bitrate;
        
        @JsonProperty("emotion")
        private String emotion;
        
        @JsonProperty("enable_emotion")
        private Boolean enableEmotion;
        
        @JsonProperty("emotion_scale")
        private Double emotionScale;
        
        @JsonProperty("explicit_language")
        private String explicitLanguage;
        
        @JsonProperty("context_language")
        private String contextLanguage;
        
        @JsonProperty("loudness_ratio")
        private Double loudnessRatio;
    }
    
    @Data
    public static class Request {
        @JsonProperty("reqid")
        private String reqId;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("operation")
        private String operation;
        
        @JsonProperty("model")
        private String model;
        
        @JsonProperty("text_type")
        private String textType;
        
        @JsonProperty("silence_duration")
        private Double silenceDuration;
        
        @JsonProperty("with_timestamp")
        private Integer withTimestamp;
        
        @JsonProperty("extra_param")
        private String extraParam;
    }
}
