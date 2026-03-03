package com.aicreation.service;

import com.aicreation.entity.bo.AudioFileRecordBo;

/**
 * 音频文件记录服务
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
public interface AudioFileRecordService {
    
    /**
     * 创建音频文件记录
     * 
     * @param reqId 请求ID
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @param fileType 文件类型
     * @param voiceType 音色类型
     * @param textContent 文本内容
     * @param synthesisType 合成类型
     * @return 音频文件记录
     */
    AudioFileRecordBo createRecord(String reqId, String fileName, String filePath, 
                                 Long fileSize, String fileType, String voiceType, 
                                 String textContent, String synthesisType);
    
    /**
     * 根据请求ID查询音频文件记录
     * 
     * @param reqId 请求ID
     * @return 音频文件记录
     */
    AudioFileRecordBo getByReqId(String reqId);
    
    /**
     * 更新音频文件记录状态
     * 
     * @param reqId 请求ID
     * @param status 状态
     * @param errorMessage 错误信息
     */
    void updateStatus(String reqId, String status, String errorMessage);
    
    /**
     * 删除音频文件记录
     * 
     * @param reqId 请求ID
     * @return 是否删除成功
     */
    boolean deleteRecord(String reqId);
    
    /**
     * 更新音频文件记录
     * 
     * @param record 音频文件记录
     */
    void updateRecord(AudioFileRecordBo record);
}
