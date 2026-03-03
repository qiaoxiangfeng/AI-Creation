package com.aicreation.service.impl;

import com.aicreation.controller.dto.*;
import com.aicreation.enums.AsyncTaskStatusEnum;
import com.aicreation.enums.SynthesisTypeEnum;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.external.VolcengineTtsClient;
import com.aicreation.external.dto.TtsResponse;
import com.aicreation.service.AudioFileService;
import com.aicreation.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 语音合成服务实现
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Slf4j
@Service
public class TtsServiceImpl implements TtsService {
    
    @Autowired
    private VolcengineTtsClient volcengineTtsClient;
    
    @Autowired
    private AudioFileService audioFileService;
    
    @Override
    public TtsSynthesizeRespDto synthesizeText(TtsSynthesizeReqDto request) {
        log.info("收到语音合成请求，文本长度: {}, 音色类型: {}, 保存文件: {}", 
            request.getText().length(), request.getVoiceType(), request.getSaveFile());
        
        TtsResponse response = volcengineTtsClient.synthesizeText(request.getText(), request.getVoiceType());
        
        if (response.getCode() == 3000) {
            log.info("语音合成成功，请求ID: {}", response.getReqId());
            
            String filePath = null;
            // 保存音频文件
            if (request.getSaveFile() && response.getData() != null) {
                try {
                    filePath = audioFileService.saveAudioFile(
                        response.getData(), 
                        response.getReqId(), 
                        "mp3",
                        request.getVoiceType(),
                        request.getText(),
                        SynthesisTypeEnum.HTTP.getCode()
                    );
                    log.info("音频文件保存成功: {}", filePath);
                } catch (Exception e) {
                    log.warn("保存音频文件失败，请求ID: {}", response.getReqId(), e);
                    // 不影响主要功能，继续返回合成结果
                }
            }
            
            return TtsSynthesizeRespDto.fromTtsResponse(response, filePath);
        } else {
            log.warn("语音合成失败，错误码: {}, 错误信息: {}", response.getCode(), response.getMessage());
            throw new BusinessException(ErrorCodeEnum.INVALID_REQUEST.getCode(), response.getMessage());
        }
    }
    
    @Override
    public TtsAsyncRespDto synthesizeTextAsync(TtsAsyncReqDto request) {
        log.info("收到异步语音合成请求，文本长度: {}, 音色类型: {}", 
            request.getText().length(), request.getVoiceType());
        
        CompletableFuture<TtsResponse> future = volcengineTtsClient.synthesizeTextAsync(request.getText(), request.getVoiceType());
        
        // 异步处理，返回任务ID
        String taskId = UUID.randomUUID().toString();
        
        future.thenAccept(response -> {
            if (response.getCode() == 3000) {
                log.info("异步语音合成成功，任务ID: {}, 请求ID: {}", taskId, response.getReqId());
            } else {
                log.warn("异步语音合成失败，任务ID: {}, 错误码: {}, 错误信息: {}", 
                    taskId, response.getCode(), response.getMessage());
            }
        }).exceptionally(throwable -> {
            log.error("异步语音合成异常，任务ID: {}", taskId, throwable);
            return null;
        });
        
        TtsAsyncRespDto respDto = new TtsAsyncRespDto();
        respDto.setTaskId(taskId);
        respDto.setStatus(AsyncTaskStatusEnum.SUBMITTED.getCode());
        respDto.setMessage("异步合成任务已提交");
        
        return respDto;
    }
    
    @Override
    public SseEmitter synthesizeTextStream(TtsStreamReqDto request) {
        log.info("收到流式语音合成请求，文本长度: {}, 音色类型: {}, 保存文件: {}", 
            request.getText().length(), request.getVoiceType(), request.getSaveFile());
        
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 异步执行流式合成
        CompletableFuture.runAsync(() -> {
            volcengineTtsClient.synthesizeTextStream(request.getText(), request.getVoiceType(), 
                response -> {
                    try {
                        // 发送音频数据
                        emitter.send(SseEmitter.event()
                            .name("audio")
                            .data(response.getData()));
                        
                        // 保存音频文件（流式追加）
                        if (request.getSaveFile() && response.getData() != null) {
                            try {
                                boolean isFirst = response.getSequence() != null && response.getSequence() == 1;
                                audioFileService.saveStreamAudioFile(
                                    response.getData(), 
                                    response.getReqId(), 
                                    "mp3",
                                    isFirst,
                                    request.getVoiceType(),
                                    request.getText(),
                                    SynthesisTypeEnum.STREAM.getCode()
                                );
                                log.debug("流式音频数据保存成功，序号: {}", response.getSequence());
                            } catch (Exception e) {
                                log.warn("保存流式音频数据失败，序号: {}", response.getSequence(), e);
                            }
                        }
                        
                        log.debug("发送音频数据，序号: {}, 是否最后一段: {}", 
                            response.getSequence(), response.getSequence() != null && response.getSequence() < 0);
                        
                        if (response.getSequence() != null && response.getSequence() < 0) {
                            // 更新流式合成完成状态
                            if (request.getSaveFile()) {
                                audioFileService.updateStreamStatus(response.getReqId(), "SUCCESS", null);
                            }
                            
                            emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("合成完成"));
                            emitter.complete();
                            log.info("流式语音合成完成");
                        }
                    } catch (IOException e) {
                        log.error("发送SSE数据失败", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data(error));
                        emitter.completeWithError(new RuntimeException(error));
                    } catch (IOException e) {
                        log.error("发送错误信息失败", e);
                        emitter.completeWithError(e);
                    }
                });
        });
        
        return emitter;
    }
    
    @Override
    public TtsAudioInfoRespDto getAudioFileInfo(String reqId) {
        log.info("获取音频文件信息请求，请求ID: {}", reqId);
        
        String filePath = audioFileService.getAudioFilePath(reqId);
        if (filePath == null) {
            throw new BusinessException("4040", "音频文件不存在");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException("4040", "音频文件不存在");
        }
        
        TtsAudioInfoRespDto fileInfo = new TtsAudioInfoRespDto();
        fileInfo.setReqId(reqId);
        fileInfo.setFilePath(filePath);
        fileInfo.setFileName(file.getName());
        fileInfo.setFileSize(file.length());
        fileInfo.setLastModified(file.lastModified());
        fileInfo.setExists(true);
        
        log.info("返回音频文件信息，请求ID: {}, 文件大小: {} bytes", reqId, file.length());
        
        return fileInfo;
    }
    
    @Override
    public TtsDeleteRespDto deleteAudioFile(String reqId) {
        log.info("删除音频文件请求，请求ID: {}", reqId);
        
        boolean deleted = audioFileService.deleteAudioFile(reqId);
        
        TtsDeleteRespDto respDto = new TtsDeleteRespDto();
        respDto.setReqId(reqId);
        respDto.setSuccess(deleted);
        
        if (deleted) {
            log.info("音频文件删除成功，请求ID: {}", reqId);
            respDto.setMessage("音频文件删除成功");
        } else {
            log.warn("音频文件不存在或删除失败，请求ID: {}", reqId);
            respDto.setMessage("音频文件不存在或删除失败");
            throw new BusinessException("4040", "音频文件不存在或删除失败");
        }
        
        return respDto;
    }
    
    @Override
    public ResponseEntity<Resource> getAudioFile(String reqId) {
        log.info("获取音频文件请求，请求ID: {}", reqId);
        
        String filePath = audioFileService.getAudioFilePath(reqId);
        if (filePath == null) {
            throw new BusinessException("4040", "音频文件不存在，请求ID: " + reqId);
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException("4040", "音频文件不存在，路径: " + filePath);
        }
        
        Resource resource = new FileSystemResource(file);
        
        // 根据文件扩展名设置Content-Type
        String contentType;
        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            log.warn("无法探测文件类型，使用默认类型，请求ID: {}", reqId, e);
            contentType = "application/octet-stream";
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        log.info("返回音频文件，请求ID: {}, 文件大小: {} bytes", reqId, file.length());
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
            .body(resource);
    }
}
