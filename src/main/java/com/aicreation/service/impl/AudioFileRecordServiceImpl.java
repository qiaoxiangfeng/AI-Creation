package com.aicreation.service.impl;

import com.aicreation.entity.bo.AudioFileRecordBo;
import com.aicreation.entity.po.AudioFileRecord;
import com.aicreation.enums.AudioFileStatusEnum;
import com.aicreation.enums.AudioFileTypeEnum;
import com.aicreation.enums.SynthesisTypeEnum;
import com.aicreation.mapper.AudioFileRecordConverter;
import com.aicreation.mapper.AudioFileRecordMapper;
import com.aicreation.service.AudioFileRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 音频文件记录服务实现
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Slf4j
@Service
public class AudioFileRecordServiceImpl implements AudioFileRecordService {
    
    @Autowired
    private AudioFileRecordMapper audioFileRecordMapper;
    
    @Override
    public AudioFileRecordBo createRecord(String reqId, String fileName, String filePath, 
                                        Long fileSize, String fileType, String voiceType, 
                                        String textContent, String synthesisType) {
        log.info("创建音频文件记录，请求ID: {}, 文件名: {}", reqId, fileName);
        
        AudioFileRecord po = new AudioFileRecord();
        po.setReqId(reqId);
        po.setFileName(fileName);
        po.setFilePath(filePath);
        po.setFileSize(fileSize);
        po.setFileType(AudioFileTypeEnum.fromCode(fileType));
        po.setVoiceType(voiceType);
        po.setTextContent(textContent);
        po.setSynthesisType(SynthesisTypeEnum.fromCode(synthesisType));
        po.setStatus(AudioFileStatusEnum.PROCESSING);
        po.setCreateTime(LocalDateTime.now());
        po.setUpdateTime(LocalDateTime.now());
        po.setCreatedBy("system");
        po.setUpdatedBy("system");
        
        int result = audioFileRecordMapper.insert(po);
        if (result > 0) {
            log.info("音频文件记录创建成功，请求ID: {}", reqId);
            return AudioFileRecordConverter.INSTANCE.poToBo(po);
        } else {
            log.error("音频文件记录创建失败，请求ID: {}", reqId);
            throw new RuntimeException("创建音频文件记录失败");
        }
    }
    
    @Override
    public AudioFileRecordBo getByReqId(String reqId) {
        log.debug("查询音频文件记录，请求ID: {}", reqId);
        
        AudioFileRecord po = audioFileRecordMapper.selectByReqId(reqId);
        if (po != null) {
            return AudioFileRecordConverter.INSTANCE.poToBo(po);
        }
        return null;
    }
    
    @Override
    public void updateStatus(String reqId, String status, String errorMessage) {
        log.info("更新音频文件记录状态，请求ID: {}, 状态: {}", reqId, status);
        
        AudioFileStatusEnum audioFileStatus = AudioFileStatusEnum.fromCode(status);
        int result = audioFileRecordMapper.updateStatusByReqId(reqId, audioFileStatus.getCode(), errorMessage);
        if (result > 0) {
            log.info("音频文件记录状态更新成功，请求ID: {}", reqId);
        } else {
            log.warn("音频文件记录状态更新失败，请求ID: {}", reqId);
        }
    }
    
    @Override
    public boolean deleteRecord(String reqId) {
        log.info("删除音频文件记录，请求ID: {}", reqId);
        
        int result = audioFileRecordMapper.deleteByReqId(reqId);
        if (result > 0) {
            log.info("音频文件记录删除成功，请求ID: {}", reqId);
            return true;
        } else {
            log.warn("音频文件记录删除失败，请求ID: {}", reqId);
            return false;
        }
    }
    
    @Override
    public void updateRecord(AudioFileRecordBo record) {
        log.info("更新音频文件记录，请求ID: {}", record.getReqId());
        
        AudioFileRecord po = AudioFileRecordConverter.INSTANCE.boToPo(record);
        po.setUpdateTime(LocalDateTime.now());
        po.setUpdatedBy("system");
        
        int result = audioFileRecordMapper.updateByReqId(po);
        if (result > 0) {
            log.info("音频文件记录更新成功，请求ID: {}", record.getReqId());
        } else {
            log.warn("音频文件记录更新失败，请求ID: {}", record.getReqId());
        }
    }
}
