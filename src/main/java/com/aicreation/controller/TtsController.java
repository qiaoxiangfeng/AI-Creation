package com.aicreation.controller;

import com.aicreation.controller.dto.*;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

/**
 * 语音合成控制器
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@RestController
@RequestMapping("/tts")
@Tag(name = "语音合成", description = "豆包语音合成相关接口")
public class TtsController {
    
    @Autowired
    private TtsService ttsService;
    
    @PostMapping("/synthesize")
    @Operation(summary = "HTTP方式合成语音", description = "使用HTTP接口进行非流式语音合成")
    public BaseResponse<TtsSynthesizeRespDto> synthesizeText(@Valid @RequestBody TtsSynthesizeReqDto request) {
        TtsSynthesizeRespDto response = ttsService.synthesizeText(request);
        return BaseResponse.success(response);
    }
    
    @PostMapping("/synthesize/async")
    @Operation(summary = "异步合成语音", description = "使用异步方式进行语音合成")
    public BaseResponse<TtsAsyncRespDto> synthesizeTextAsync(@Valid @RequestBody TtsAsyncReqDto request) {
        TtsAsyncRespDto response = ttsService.synthesizeTextAsync(request);
        return BaseResponse.success(response);
    }
    
    @PostMapping("/synthesize/stream")
    @Operation(summary = "流式合成语音", description = "使用WebSocket进行流式语音合成")
    public SseEmitter synthesizeTextStream(@Valid @RequestBody TtsStreamReqDto request) {
        return ttsService.synthesizeTextStream(request);
    }
    
    @GetMapping("/audio/{reqId}")
    @Operation(summary = "获取音频文件", description = "根据请求ID获取合成的音频文件")
    public ResponseEntity<Resource> getAudioFile(
            @Parameter(description = "请求ID") @PathVariable String reqId) {
        return ttsService.getAudioFile(reqId);
    }
    
    @GetMapping("/audio/{reqId}/info")
    @Operation(summary = "获取音频文件信息", description = "根据请求ID获取音频文件的基本信息")
    public BaseResponse<TtsAudioInfoRespDto> getAudioFileInfo(
            @Parameter(description = "请求ID") @PathVariable String reqId) {
        TtsAudioInfoRespDto response = ttsService.getAudioFileInfo(reqId);
        return BaseResponse.success(response);
    }
    
    @DeleteMapping("/audio/{reqId}")
    @Operation(summary = "删除音频文件", description = "根据请求ID删除合成的音频文件")
    public BaseResponse<TtsDeleteRespDto> deleteAudioFile(
            @Parameter(description = "请求ID") @PathVariable String reqId) {
        TtsDeleteRespDto response = ttsService.deleteAudioFile(reqId);
        return BaseResponse.success(response);
    }
    
}
