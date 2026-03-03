package com.aicreation.external.impl;

import com.aicreation.external.VolcengineTtsClient;
import com.aicreation.external.config.DouBaoTtsProperties;
import com.aicreation.external.dto.TtsRequest;
import com.aicreation.external.dto.TtsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 火山引擎语音合成客户端实现（基于官方文档）
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Slf4j
@Service
public class VolcengineTtsClientImpl implements VolcengineTtsClient {
    
    @Autowired
    private DouBaoTtsProperties ttsProperties;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public TtsResponse synthesizeText(TtsRequest request) {
        try {
            log.info("开始HTTP语音合成，请求ID: {}", request.getRequest().getReqId());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer; " + ttsProperties.getAccessToken());
            
            HttpEntity<TtsRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<TtsResponse> response = restTemplate.exchange(
                ttsProperties.getHttpUrl(),
                HttpMethod.POST,
                entity,
                TtsResponse.class
            );
            
            TtsResponse result = response.getBody();
            log.info("HTTP语音合成完成，请求ID: {}, 状态码: {}", 
                request.getRequest().getReqId(), 
                result != null ? result.getCode() : "null");
            
            return result;
        } catch (Exception e) {
            log.error("HTTP语音合成失败，请求ID: {}", request.getRequest().getReqId(), e);
            throw new RuntimeException("语音合成失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public CompletableFuture<TtsResponse> synthesizeTextAsync(TtsRequest request) {
        return CompletableFuture.supplyAsync(() -> synthesizeText(request));
    }
    
    @Override
    public void synthesizeTextStream(TtsRequest request, Consumer<TtsResponse> onResponse, Consumer<String> onError) {
        try {
            log.info("开始WebSocket流式语音合成，请求ID: {}", request.getRequest().getReqId());
            
            CountDownLatch latch = new CountDownLatch(1);
            
            WebSocketClient client = new WebSocketClient(URI.create(ttsProperties.getWsUrl())) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("WebSocket连接已建立，请求ID: {}", request.getRequest().getReqId());
                    
                    try {
                        // 发送合成请求
                        String requestJson = objectMapper.writeValueAsString(request);
                        send(requestJson);
                    } catch (Exception e) {
                        log.error("发送WebSocket请求失败", e);
                        onError.accept("发送请求失败: " + e.getMessage());
                        latch.countDown();
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    log.debug("收到WebSocket消息: {}", message);
                    
                    try {
                        TtsResponse response = objectMapper.readValue(message, TtsResponse.class);
                        
                        if (response.getCode() != null && response.getCode() == 3000) {
                            // 成功响应
                            onResponse.accept(response);
                            
                            // 检查是否为最后一条消息
                            if (response.getSequence() != null && response.getSequence() < 0) {
                                log.info("WebSocket流式语音合成完成，请求ID: {}", request.getRequest().getReqId());
                                latch.countDown();
                            }
                        } else {
                            // 错误响应
                            String errorMsg = String.format("合成失败，错误码: %d, 错误信息: %s", 
                                response.getCode(), response.getMessage());
                            log.error("WebSocket语音合成失败，请求ID: {}, {}", request.getRequest().getReqId(), errorMsg);
                            onError.accept(errorMsg);
                            latch.countDown();
                        }
                    } catch (Exception e) {
                        log.error("解析WebSocket响应失败，请求ID: {}", request.getRequest().getReqId(), e);
                        onError.accept("解析响应失败: " + e.getMessage());
                        latch.countDown();
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("WebSocket连接已关闭，请求ID: {}, 状态码: {}, 原因: {}", 
                        request.getRequest().getReqId(), code, reason);
                    latch.countDown();
                }
                
                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket传输错误，请求ID: {}", request.getRequest().getReqId(), ex);
                    onError.accept("传输错误: " + ex.getMessage());
                    latch.countDown();
                }
            };
            
            // 设置认证头
            client.addHeader("Authorization", "Bearer; " + ttsProperties.getAccessToken());
            
            // 连接WebSocket
            client.connect();
            
            // 等待合成完成
            latch.await(60, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("WebSocket流式语音合成失败，请求ID: {}", request.getRequest().getReqId(), e);
            onError.accept("流式合成失败: " + e.getMessage());
        }
    }
    
    @Override
    public TtsResponse synthesizeText(String text, String voiceType) {
        TtsRequest request = buildDefaultRequest(text, voiceType);
        return synthesizeText(request);
    }
    
    @Override
    public CompletableFuture<TtsResponse> synthesizeTextAsync(String text, String voiceType) {
        TtsRequest request = buildDefaultRequest(text, voiceType);
        return synthesizeTextAsync(request);
    }
    
    @Override
    public void synthesizeTextStream(String text, String voiceType, Consumer<TtsResponse> onResponse, Consumer<String> onError) {
        TtsRequest request = buildStreamRequest(text, voiceType);
        synthesizeTextStream(request, onResponse, onError);
    }
    
    @Override
    public TtsRequest buildDefaultRequest(String text, String voiceType) {
        TtsRequest request = new TtsRequest();
        
        // 应用配置
        TtsRequest.App app = new TtsRequest.App();
        app.setAppId(ttsProperties.getAppId());
        app.setToken(ttsProperties.getAccessToken());
        app.setCluster(ttsProperties.getCluster());
        request.setApp(app);
        
        // 用户配置
        TtsRequest.User user = new TtsRequest.User();
        user.setUid("default_user");
        request.setUser(user);
        
        // 音频配置
        TtsRequest.Audio audio = new TtsRequest.Audio();
        audio.setVoiceType(voiceType != null ? voiceType : ttsProperties.getDefaultVoiceType());
        audio.setEncoding(ttsProperties.getDefaultEncoding());
        audio.setSpeedRatio(ttsProperties.getDefaultSpeedRatio());
        audio.setRate(ttsProperties.getDefaultRate());
        audio.setBitrate(ttsProperties.getDefaultBitrate());
        request.setAudio(audio);
        
        // 请求配置
        TtsRequest.Request requestConfig = new TtsRequest.Request();
        requestConfig.setReqId(UUID.randomUUID().toString());
        requestConfig.setText(text);
        requestConfig.setOperation("query");
        request.setRequest(requestConfig);
        
        return request;
    }
    
    @Override
    public TtsRequest buildStreamRequest(String text, String voiceType) {
        TtsRequest request = buildDefaultRequest(text, voiceType);
        request.getRequest().setOperation("submit");
        return request;
    }
}