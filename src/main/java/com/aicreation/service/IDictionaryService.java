package com.aicreation.service;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public interface IDictionaryService {

    /**
     * 根据ID查询字典信息
     *
     * @param request 查询请求
     * @return 字典信息，如果不存在返回null
     */
    DictionaryRespDto getDictionaryById(DictionaryQueryReqDto request);

    /**
     * 根据字典键查询字典列表
     *
     * @param dictKey 字典键
     * @return 字典列表
     */
    List<DictionaryRespDto> getDictionaryByKey(String dictKey);

    /**
     * 获取所有唯一的字典键
     *
     * @return 字典键列表
     */
    List<String> getAllDistinctDictKeys();

    /**
     * 创建字典
     *
     * @param request 创建请求
     * @return 创建后的字典ID
     */
    Long createDictionary(DictionaryCreateReqDto request);

    /**
     * 更新字典信息
     *
     * @param request 更新请求
     * @return 是否更新成功
     */
    Boolean updateDictionary(DictionaryUpdateReqDto request);

    /**
     * 删除字典
     *
     * @param request 删除请求
     * @return 是否删除成功
     */
    Boolean deleteDictionary(DictionaryDeleteReqDto request);

    /**
     * 查询字典列表（分页）
     *
     * @param request 查询请求
     * @return 字典列表
     */
    PageRespDto<DictionaryListRespDto> getDictionaryList(DictionaryListReqDto request);
}