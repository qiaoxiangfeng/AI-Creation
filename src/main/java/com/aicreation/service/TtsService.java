package com.aicreation.service;

import com.aicreation.controller.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 语音合成服务接口
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public interface TtsService {
    
    /**
     * HTTP方式合成语音
     * 
     * @param request 合成请求
     * @return 合成响应
     */
    TtsSynthesizeRespDto synthesizeText(TtsSynthesizeReqDto request);
    
    /**
     * 异步合成语音
     * 
     * @param request 异步合成请求
     * @return 异步合成响应
     */
    TtsAsyncRespDto synthesizeTextAsync(TtsAsyncReqDto request);
    
    /**
     * 流式合成语音
     * 
     * @param request 流式合成请求
     * @return SSE发射器
     */
    SseEmitter synthesizeTextStream(TtsStreamReqDto request);
    
    /**
     * 获取音频文件信息
     * 
     * @param reqId 请求ID
     * @return 音频文件信息
     */
    TtsAudioInfoRespDto getAudioFileInfo(String reqId);
    
    /**
     * 删除音频文件
     * 
     * @param reqId 请求ID
     * @return 删除响应
     */
    TtsDeleteRespDto deleteAudioFile(String reqId);
    
    /**
     * 获取音频文件（用于文件下载）
     * 
     * @param reqId 请求ID
     * @return 文件资源响应
     */
    ResponseEntity<Resource> getAudioFile(String reqId);
}
