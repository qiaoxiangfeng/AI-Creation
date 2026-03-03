package com.aicreation.mapper;

import com.aicreation.entity.bo.AudioFileRecordBo;
import com.aicreation.entity.po.AudioFileRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 音频文件记录转换器
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Mapper
public interface AudioFileRecordConverter {
    
    AudioFileRecordConverter INSTANCE = Mappers.getMapper(AudioFileRecordConverter.class);
    
    /**
     * PO转BO
     * 
     * @param po PO对象
     * @return BO对象
     */
    AudioFileRecordBo poToBo(AudioFileRecord po);
    
    /**
     * BO转PO
     * 
     * @param bo BO对象
     * @return PO对象
     */
    AudioFileRecord boToPo(AudioFileRecordBo bo);
}
