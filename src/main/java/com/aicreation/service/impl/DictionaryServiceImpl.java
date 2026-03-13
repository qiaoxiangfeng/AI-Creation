package com.aicreation.service.impl;

import com.aicreation.entity.bo.DictionaryBo;
import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.Dictionary;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.DictionaryMapper;
import com.aicreation.service.IDictionaryService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字典服务实现类
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements IDictionaryService {

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Override
    public DictionaryRespDto getDictionaryById(DictionaryQueryReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("查询字典失败：字典ID为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Dictionary dictionary = dictionaryMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(dictionary)) {
            return null;
        }

        return convertToRespDto(dictionary);
    }

    @Override
    public List<DictionaryRespDto> getDictionaryByKey(String dictKey) {
        if (!StringUtils.hasText(dictKey)) {
            log.warn("查询字典失败：字典键为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        List<Dictionary> dictionaries = dictionaryMapper.selectByDictKey(dictKey);
        return dictionaries.stream()
                .map(this::convertToRespDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllDistinctDictKeys() {
        List<Dictionary> dictionaries = dictionaryMapper.selectAll();
        return dictionaries.stream()
                .map(Dictionary::getDictKey)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public Long createDictionary(DictionaryCreateReqDto request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getDictKey()) || !StringUtils.hasText(request.getDictValue())) {
            log.warn("创建字典失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 转换为业务对象
        DictionaryBo dictionaryBo = new DictionaryBo();
        dictionaryBo.setDictKey(request.getDictKey());
        dictionaryBo.setDictValue(request.getDictValue());
        dictionaryBo.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        dictionaryBo.setResState(1);
        dictionaryBo.setCreateTime(LocalDateTime.now());
        dictionaryBo.setUpdateTime(LocalDateTime.now());

        // 转换为持久化对象
        Dictionary dictionary = new Dictionary();
        dictionary.setDictKey(dictionaryBo.getDictKey());
        dictionary.setDictValue(dictionaryBo.getDictValue());
        dictionary.setSortOrder(dictionaryBo.getSortOrder());
        dictionary.setResState(dictionaryBo.getResState());
        dictionary.setCreateTime(dictionaryBo.getCreateTime());
        dictionary.setUpdateTime(dictionaryBo.getUpdateTime());

        // 保存到数据库
        int result = dictionaryMapper.insert(dictionary);
        if (result <= 0) {
            log.error("创建字典失败：数据库插入失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return dictionary.getId();
    }

    @Override
    public Boolean updateDictionary(DictionaryUpdateReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("更新字典失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查字典是否存在
        Dictionary existingDictionary = dictionaryMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(existingDictionary)) {
            log.warn("更新字典失败：字典不存在，id={}", request.getId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 转换为持久化对象
        Dictionary dictionary = new Dictionary();
        dictionary.setId(request.getId());
        if (StringUtils.hasText(request.getDictKey())) {
            dictionary.setDictKey(request.getDictKey());
        }
        if (StringUtils.hasText(request.getDictValue())) {
            dictionary.setDictValue(request.getDictValue());
        }
        if (request.getSortOrder() != null) {
            dictionary.setSortOrder(request.getSortOrder());
        }
        dictionary.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        int result = dictionaryMapper.updateByPrimaryKey(dictionary);
        if (result <= 0) {
            log.error("更新字典失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public Boolean deleteDictionary(DictionaryDeleteReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("删除字典失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查字典是否存在
        Dictionary existingDictionary = dictionaryMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(existingDictionary)) {
            log.warn("删除字典失败：字典不存在，id={}", request.getId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 软删除字典
        int result = dictionaryMapper.deleteByPrimaryKey(request.getId());
        if (result <= 0) {
            log.error("删除字典失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public PageRespDto<DictionaryListRespDto> getDictionaryList(DictionaryListReqDto request) {
        if (Objects.isNull(request)) {
            log.warn("查询字典列表失败：请求参数为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 设置分页
        PageHelper.startPage(request.getPageNo(), request.getPageSize());

        // 查询字典列表
        List<Dictionary> dictionaries = dictionaryMapper.selectDictionaryList(
                request.getDictKey(),
                request.getDictValue()
        );

        // 获取分页信息
        PageInfo<Dictionary> pageInfo = new PageInfo<>(dictionaries);

        // 转换为列表响应DTO
        List<DictionaryListRespDto> dictionaryListRespDtos = dictionaries.stream()
                .map(this::convertToListRespDto)
                .collect(Collectors.toList());

        // 构建分页响应
        PageRespDto<DictionaryListRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setPageNo(pageInfo.getPageNum());
        pageRespDto.setPageSize(pageInfo.getPageSize());
        pageRespDto.setSize(pageInfo.getSize());
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setPages(pageInfo.getPages());
        pageRespDto.setList(dictionaryListRespDtos);

        return pageRespDto;
    }

    /**
     * 转换为响应DTO
     */
    private DictionaryRespDto convertToRespDto(Dictionary dictionary) {
        DictionaryRespDto dto = new DictionaryRespDto();
        dto.setId(dictionary.getId());
        dto.setDictKey(dictionary.getDictKey());
        dto.setDictValue(dictionary.getDictValue());
        dto.setSortOrder(dictionary.getSortOrder());
        dto.setCreateTime(dictionary.getCreateTime());
        dto.setUpdateTime(dictionary.getUpdateTime());
        return dto;
    }

    /**
     * 转换为列表响应DTO
     */
    private DictionaryListRespDto convertToListRespDto(Dictionary dictionary) {
        DictionaryListRespDto dto = new DictionaryListRespDto();
        dto.setId(dictionary.getId());
        dto.setDictKey(dictionary.getDictKey());
        dto.setDictValue(dictionary.getDictValue());
        dto.setSortOrder(dictionary.getSortOrder());
        dto.setCreateTime(dictionary.getCreateTime());
        return dto;
    }
}