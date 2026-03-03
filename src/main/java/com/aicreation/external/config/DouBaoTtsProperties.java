package com.aicreation.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 豆包语音合成配置属性
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@ConfigurationProperties(prefix = "app.doubao.tts")
public class DouBaoTtsProperties {
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 业务集群
     */
    private String cluster = "volcano_tts";
    
    /**
     * WebSocket连接地址
     */
    private String wsUrl = "wss://openspeech.bytedance.com/api/v3/tts/unidirectional/stream";
    
    /**
     * HTTP接口地址
     */
    private String httpUrl = "https://openspeech.bytedance.com/api/v1/tts";
    
    /**
     * 默认音色类型
     */
    private String defaultVoiceType = "zh_female_cancan_mars_bigtts";
    
    /**
     * 默认音频编码格式
     */
    private String defaultEncoding = "mp3";
    
    /**
     * 默认语速
     */
    private Double defaultSpeedRatio = 1.0;
    
    /**
     * 默认采样率
     */
    private Integer defaultRate = 24000;
    
    /**
     * 默认比特率
     */
    private Integer defaultBitrate = 160;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getDefaultVoiceType() {
        return defaultVoiceType;
    }

    public void setDefaultVoiceType(String defaultVoiceType) {
        this.defaultVoiceType = defaultVoiceType;
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public Double getDefaultSpeedRatio() {
        return defaultSpeedRatio;
    }

    public void setDefaultSpeedRatio(Double defaultSpeedRatio) {
        this.defaultSpeedRatio = defaultSpeedRatio;
    }

    public Integer getDefaultRate() {
        return defaultRate;
    }

    public void setDefaultRate(Integer defaultRate) {
        this.defaultRate = defaultRate;
    }

    public Integer getDefaultBitrate() {
        return defaultBitrate;
    }

    public void setDefaultBitrate(Integer defaultBitrate) {
        this.defaultBitrate = defaultBitrate;
    }
}
