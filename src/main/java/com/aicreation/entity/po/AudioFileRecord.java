package com.aicreation.entity.po;

import com.aicreation.enums.AudioFileStatusEnum;
import com.aicreation.enums.AudioFileTypeEnum;
import com.aicreation.enums.SynthesisTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 音频文件记录实体
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Data
public class AudioFileRecord {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 请求ID
     */
    private String reqId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private AudioFileTypeEnum fileType;
    
    /**
     * 音色类型
     */
    private String voiceType;
    
    /**
     * 合成文本内容
     */
    private String textContent;
    
    /**
     * 合成类型
     */
    private SynthesisTypeEnum synthesisType;
    
    /**
     * 状态
     */
    private AudioFileStatusEnum status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 更新人
     */
    private String updatedBy;
}
