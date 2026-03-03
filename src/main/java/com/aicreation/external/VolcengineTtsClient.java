package com.aicreation.external;

import com.aicreation.external.dto.TtsRequest;
import com.aicreation.external.dto.TtsResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 火山引擎语音合成客户端接口（基于官方文档）
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public interface VolcengineTtsClient {
    
    /**
     * HTTP方式合成语音（非流式）
     * 
     * @param request 合成请求
     * @return 合成结果
     */
    TtsResponse synthesizeText(TtsRequest request);
    
    /**
     * HTTP方式合成语音（异步）
     * 
     * @param request 合成请求
     * @return 合成结果
     */
    CompletableFuture<TtsResponse> synthesizeTextAsync(TtsRequest request);
    
    /**
     * WebSocket方式合成语音（流式）
     * 
     * @param request 合成请求
     * @param onResponse 响应回调
     * @param onError 错误回调
     */
    void synthesizeTextStream(TtsRequest request, Consumer<TtsResponse> onResponse, Consumer<String> onError);
    
    /**
     * 简化的文本合成方法
     * 
     * @param text 要合成的文本
     * @param voiceType 音色类型
     * @return 合成结果
     */
    TtsResponse synthesizeText(String text, String voiceType);
    
    /**
     * 简化的文本合成方法（异步）
     * 
     * @param text 要合成的文本
     * @param voiceType 音色类型
     * @return 合成结果
     */
    CompletableFuture<TtsResponse> synthesizeTextAsync(String text, String voiceType);
    
    /**
     * 简化的流式文本合成方法
     * 
     * @param text 要合成的文本
     * @param voiceType 音色类型
     * @param onResponse 响应回调
     * @param onError 错误回调
     */
    void synthesizeTextStream(String text, String voiceType, Consumer<TtsResponse> onResponse, Consumer<String> onError);
    
    /**
     * 构建默认请求
     * 
     * @param text 要合成的文本
     * @param voiceType 音色类型
     * @return 请求对象
     */
    TtsRequest buildDefaultRequest(String text, String voiceType);
    
    /**
     * 构建流式请求
     * 
     * @param text 要合成的文本
     * @param voiceType 音色类型
     * @return 请求对象
     */
    TtsRequest buildStreamRequest(String text, String voiceType);
}
