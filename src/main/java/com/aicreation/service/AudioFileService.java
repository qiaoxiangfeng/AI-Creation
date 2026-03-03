package com.aicreation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 音频文件服务
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Slf4j
@Service
public class AudioFileService {
    
    @Value("${app.audio.file-path:file}")
    private String filePath;
    
    @Value("${app.audio.file-prefix:audio}")
    private String filePrefix;
    
    @Autowired
    private AudioFileRecordService audioFileRecordService;
    
    /**
     * 保存音频文件
     * 
     * @param audioData Base64编码的音频数据
     * @param reqId 请求ID
     * @param encoding 音频编码格式
     * @return 保存的文件路径
     */
    public String saveAudioFile(String audioData, String reqId, String encoding) {
        return saveAudioFile(audioData, reqId, encoding, null, null, null);
    }
    
    /**
     * 保存音频文件（带记录信息）
     * 
     * @param audioData Base64编码的音频数据
     * @param reqId 请求ID
     * @param encoding 音频编码格式
     * @param voiceType 音色类型
     * @param textContent 文本内容
     * @param synthesisType 合成类型
     * @return 保存的文件路径
     */
    public String saveAudioFile(String audioData, String reqId, String encoding, 
                              String voiceType, String textContent, String synthesisType) {
        if (!StringUtils.hasText(audioData)) {
            throw new IllegalArgumentException("音频数据不能为空");
        }
        
        try {
            // 确保目录存在
            Path directory = Paths.get(filePath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("创建音频文件目录: {}", directory.toAbsolutePath());
            }
            
            // 生成文件名
            String fileName = generateFileName(reqId, encoding);
            Path filePath = directory.resolve(fileName);
            
            // 解码Base64数据
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(audioBytes);
                fos.flush();
            }
            
            log.info("音频文件保存成功: {}, 大小: {} bytes", filePath.toAbsolutePath(), audioBytes.length);
            
            // 保存数据库记录
            if (voiceType != null || textContent != null || synthesisType != null) {
                try {
                    String recordFileName = filePath.getFileName().toString();
                    audioFileRecordService.createRecord(reqId, recordFileName, filePath.toString(), 
                        (long) audioBytes.length, encoding, voiceType, textContent, synthesisType);
                    audioFileRecordService.updateStatus(reqId, "SUCCESS", null);
                } catch (Exception e) {
                    log.warn("保存音频文件记录失败, reqId: {}", reqId, e);
                    // 不影响主要功能
                }
            }
            
            return filePath.toString();
            
        } catch (IOException e) {
            log.error("保存音频文件失败, reqId: {}", reqId, e);
            // 更新数据库记录状态为失败
            if (voiceType != null || textContent != null || synthesisType != null) {
                try {
                    audioFileRecordService.updateStatus(reqId, "FAILED", e.getMessage());
                } catch (Exception ex) {
                    log.warn("更新音频文件记录状态失败, reqId: {}", reqId, ex);
                }
            }
            throw new RuntimeException("保存音频文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存流式音频文件（追加模式）
     * 
     * @param audioData Base64编码的音频数据
     * @param reqId 请求ID
     * @param encoding 音频编码格式
     * @param isFirst 是否为第一段
     * @return 保存的文件路径
     */
    public String saveStreamAudioFile(String audioData, String reqId, String encoding, boolean isFirst) {
        return saveStreamAudioFile(audioData, reqId, encoding, isFirst, null, null, null);
    }
    
    /**
     * 保存流式音频文件（追加模式，带记录信息）
     * 
     * @param audioData Base64编码的音频数据
     * @param reqId 请求ID
     * @param encoding 音频编码格式
     * @param isFirst 是否为第一段
     * @param voiceType 音色类型
     * @param textContent 文本内容
     * @param synthesisType 合成类型
     * @return 保存的文件路径
     */
    public String saveStreamAudioFile(String audioData, String reqId, String encoding, boolean isFirst,
                                    String voiceType, String textContent, String synthesisType) {
        if (!StringUtils.hasText(audioData)) {
            throw new IllegalArgumentException("音频数据不能为空");
        }
        
        try {
            // 确保目录存在
            Path directory = Paths.get(filePath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("创建音频文件目录: {}", directory.toAbsolutePath());
            }
            
            // 生成文件名
            String fileName = generateFileName(reqId, encoding);
            Path filePath = directory.resolve(fileName);
            
            // 解码Base64数据
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            
            // 保存文件（追加模式）
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile(), !isFirst)) {
                fos.write(audioBytes);
                fos.flush();
            }
            
            log.debug("流式音频数据保存成功: {}, 大小: {} bytes, 是否第一段: {}", 
                filePath.toAbsolutePath(), audioBytes.length, isFirst);
            
            // 保存数据库记录（仅在第一段时创建记录）
            if (isFirst && (voiceType != null || textContent != null || synthesisType != null)) {
                try {
                    String streamFileName = filePath.getFileName().toString();
                    audioFileRecordService.createRecord(reqId, streamFileName, filePath.toString(), 
                        (long) audioBytes.length, encoding, voiceType, textContent, synthesisType);
                } catch (Exception e) {
                    log.warn("创建流式音频文件记录失败, reqId: {}", reqId, e);
                    // 不影响主要功能
                }
            }
            
            return filePath.toString();
            
        } catch (IOException e) {
            log.error("保存流式音频文件失败, reqId: {}", reqId, e);
            // 更新数据库记录状态为失败
            if (voiceType != null || textContent != null || synthesisType != null) {
                try {
                    audioFileRecordService.updateStatus(reqId, "FAILED", e.getMessage());
                } catch (Exception ex) {
                    log.warn("更新流式音频文件记录状态失败, reqId: {}", reqId, ex);
                }
            }
            throw new RuntimeException("保存流式音频文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取音频文件路径
     * 
     * @param reqId 请求ID
     * @return 文件路径
     */
    public String getAudioFilePath(String reqId) {
        // 优先从数据库查询
        try {
            var record = audioFileRecordService.getByReqId(reqId);
            if (record != null && record.getFilePath() != null) {
                // 验证文件是否真实存在
                if (Files.exists(Paths.get(record.getFilePath()))) {
                    return record.getFilePath();
                } else {
                    log.warn("数据库记录的文件不存在, reqId: {}, 路径: {}", reqId, record.getFilePath());
                }
            }
        } catch (Exception e) {
            log.warn("从数据库查询文件路径失败, reqId: {}", reqId, e);
        }
        
        // 回退到文件系统查找
        Path directory = Paths.get(filePath);
        try {
            return Files.list(directory)
                .filter(path -> path.getFileName().toString().startsWith(filePrefix + "_" + reqId))
                .findFirst()
                .map(Path::toString)
                .orElse(null);
        } catch (IOException e) {
            log.error("查找音频文件失败, reqId: {}", reqId, e);
            return null;
        }
    }
    
    /**
     * 删除音频文件
     * 
     * @param reqId 请求ID
     * @return 是否删除成功
     */
    public boolean deleteAudioFile(String reqId) {
        try {
            String filePath = getAudioFilePath(reqId);
            boolean fileDeleted = false;
            
            if (filePath != null) {
                fileDeleted = Files.deleteIfExists(Paths.get(filePath));
                if (fileDeleted) {
                    log.info("音频文件删除成功: {}", filePath);
                }
            }
            
            // 删除数据库记录
            boolean recordDeleted = audioFileRecordService.deleteRecord(reqId);
            if (recordDeleted) {
                log.info("音频文件记录删除成功, reqId: {}", reqId);
            }
            
            return fileDeleted || recordDeleted;
        } catch (IOException e) {
            log.error("删除音频文件失败, reqId: {}", reqId, e);
            return false;
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param reqId 请求ID
     * @return 文件是否存在
     */
    public boolean fileExists(String reqId) {
        return getAudioFilePath(reqId) != null;
    }
    
    /**
     * 获取文件大小
     * 
     * @param reqId 请求ID
     * @return 文件大小（字节）
     */
    public long getFileSize(String reqId) {
        try {
            String filePath = getAudioFilePath(reqId);
            if (filePath != null) {
                return Files.size(Paths.get(filePath));
            }
            return 0;
        } catch (IOException e) {
            log.error("获取文件大小失败, reqId: {}", reqId, e);
            return 0;
        }
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String reqId, String encoding) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(encoding);
        return String.format("%s_%s_%s.%s", filePrefix, reqId, timestamp, extension);
    }
    
    /**
     * 根据编码格式获取文件扩展名
     */
    private String getFileExtension(String encoding) {
        if (encoding == null) {
            return "mp3";
        }
        
        switch (encoding.toLowerCase()) {
            case "mp3":
                return "mp3";
            case "wav":
                return "wav";
            case "pcm":
                return "pcm";
            case "ogg_opus":
                return "opus";
            default:
                return "mp3";
        }
    }
    
    /**
     * 获取音频文件目录路径
     */
    public String getAudioDirectoryPath() {
        return Paths.get(filePath).toAbsolutePath().toString();
    }
    
    /**
     * 更新流式合成状态
     * 
     * @param reqId 请求ID
     * @param status 状态
     * @param errorMessage 错误信息
     */
    public void updateStreamStatus(String reqId, String status, String errorMessage) {
        try {
            audioFileRecordService.updateStatus(reqId, status, errorMessage);
        } catch (Exception e) {
            log.warn("更新流式合成状态失败, reqId: {}", reqId, e);
        }
    }
}
