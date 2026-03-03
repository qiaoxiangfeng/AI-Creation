package com.aicreation.mapper;

import com.aicreation.entity.po.AudioFileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 音频文件记录Mapper
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Mapper
public interface AudioFileRecordMapper {
    
    /**
     * 插入音频文件记录
     * 
     * @param record 音频文件记录
     * @return 影响行数
     */
    int insert(AudioFileRecord record);
    
    /**
     * 根据请求ID查询音频文件记录
     * 
     * @param reqId 请求ID
     * @return 音频文件记录
     */
    AudioFileRecord selectByReqId(@Param("reqId") String reqId);
    
    /**
     * 根据请求ID更新音频文件记录
     * 
     * @param record 音频文件记录
     * @return 影响行数
     */
    int updateByReqId(AudioFileRecord record);
    
    /**
     * 根据请求ID删除音频文件记录
     * 
     * @param reqId 请求ID
     * @return 影响行数
     */
    int deleteByReqId(@Param("reqId") String reqId);
    
    /**
     * 根据请求ID更新状态
     * 
     * @param reqId 请求ID
     * @param status 状态
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    int updateStatusByReqId(@Param("reqId") String reqId, 
                           @Param("status") String status, 
                           @Param("errorMessage") String errorMessage);
}
