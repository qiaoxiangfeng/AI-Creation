package com.aicreation.service.impl;

import com.aicreation.entity.bo.ArticleGenerationConfigBo;
import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.service.IArticleGenerationConfigService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 文章生成配置服务实现类
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Service
public class ArticleGenerationConfigServiceImpl implements IArticleGenerationConfigService {

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Override
    public ArticleGenerationConfigRespDto getArticleGenerationConfigById(ArticleGenerationConfigQueryReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("查询文章生成配置失败：文章生成配置ID为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        ArticleGenerationConfig articleGenerationConfig = articleGenerationConfigMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(articleGenerationConfig)) {
            return null;
        }

        return convertToRespDto(articleGenerationConfig);
    }

    @Override
    public ArticleGenerationConfigRespDto getArticleGenerationConfigByTheme(String theme) {
        if (!StringUtils.hasText(theme)) {
            log.warn("查询文章生成配置失败：文章生成配置主题为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        ArticleGenerationConfig articleGenerationConfig = articleGenerationConfigMapper.selectByTheme(theme);
        if (Objects.isNull(articleGenerationConfig)) {
            return null;
        }

        return convertToRespDto(articleGenerationConfig);
    }

    @Override
    public Long createArticleGenerationConfig(ArticleGenerationConfigCreateReqDto request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getTheme())) {
            log.warn("创建文章生成配置失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章生成配置主题是否已存在
        ArticleGenerationConfig existingCategory = articleGenerationConfigMapper.selectByTheme(request.getTheme());
        if (Objects.nonNull(existingCategory)) {
            log.warn("创建文章生成配置失败：文章生成配置主题已存在，theme={}", request.getTheme());
            throw new BusinessException(ErrorCodeEnum.DUPLICATE_DATA);
        }

        // 转换为业务对象
        ArticleGenerationConfigBo categoryBo = new ArticleGenerationConfigBo();
        categoryBo.setTheme(request.getTheme());
        categoryBo.setGender(request.getGender());
        categoryBo.setGenre(request.getGenre());
        categoryBo.setPlot(request.getPlot());
        categoryBo.setCharacterType(request.getCharacterType());
        categoryBo.setStyle(request.getStyle());
        categoryBo.setAdditionalCharacteristics(request.getAdditionalCharacteristics());
        categoryBo.setPendingCount(request.getPendingCount() != null ? request.getPendingCount() : 0);
        categoryBo.setResState(1);
        categoryBo.setCreateTime(LocalDateTime.now());
        categoryBo.setUpdateTime(LocalDateTime.now());

        // 转换为持久化对象
        ArticleGenerationConfig articleGenerationConfig = new ArticleGenerationConfig();
        articleGenerationConfig.setTheme(categoryBo.getTheme());
        articleGenerationConfig.setGender(categoryBo.getGender());
        articleGenerationConfig.setGenre(categoryBo.getGenre());
        articleGenerationConfig.setPlot(categoryBo.getPlot());
        articleGenerationConfig.setCharacterType(categoryBo.getCharacterType());
        articleGenerationConfig.setStyle(categoryBo.getStyle());
        articleGenerationConfig.setAdditionalCharacteristics(categoryBo.getAdditionalCharacteristics());
        articleGenerationConfig.setPendingCount(categoryBo.getPendingCount());
        articleGenerationConfig.setResState(categoryBo.getResState());
        articleGenerationConfig.setCreateTime(categoryBo.getCreateTime());
        articleGenerationConfig.setUpdateTime(categoryBo.getUpdateTime());

        // 保存到数据库
        int result = articleGenerationConfigMapper.insert(articleGenerationConfig);
        if (result <= 0) {
            log.error("创建文章生成配置失败：数据库插入失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return articleGenerationConfig.getId();
    }

    @Override
    public Boolean updateArticleGenerationConfig(ArticleGenerationConfigUpdateReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("更新文章生成配置失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章生成配置是否存在
        ArticleGenerationConfig existingCategory = articleGenerationConfigMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(existingCategory)) {
            log.warn("更新文章生成配置失败：文章生成配置不存在，id={}", request.getId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 如果更新文章生成配置主题，检查是否与其他文章生成配置重复
        if (StringUtils.hasText(request.getTheme()) &&
            !request.getTheme().equals(existingCategory.getTheme())) {
            ArticleGenerationConfig duplicateCategory = articleGenerationConfigMapper.selectByTheme(request.getTheme());
            if (Objects.nonNull(duplicateCategory)) {
                log.warn("更新文章生成配置失败：文章生成配置主题已存在，theme={}", request.getTheme());
                throw new BusinessException(ErrorCodeEnum.DUPLICATE_DATA);
            }
        }

        // 转换为持久化对象
        ArticleGenerationConfig articleGenerationConfig = new ArticleGenerationConfig();
        articleGenerationConfig.setId(request.getId());
        if (StringUtils.hasText(request.getTheme())) {
            articleGenerationConfig.setTheme(request.getTheme());
        }
        if (StringUtils.hasText(request.getAdditionalCharacteristics())) {
            articleGenerationConfig.setAdditionalCharacteristics(request.getAdditionalCharacteristics());
        }
        if (request.getPendingCount() != null) {
            articleGenerationConfig.setPendingCount(request.getPendingCount());
        }
        articleGenerationConfig.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        int result = articleGenerationConfigMapper.updateByPrimaryKey(articleGenerationConfig);
        if (result <= 0) {
            log.error("更新文章生成配置失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public Boolean deleteArticleGenerationConfig(ArticleGenerationConfigDeleteReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            log.warn("删除文章生成配置失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章生成配置是否存在
        ArticleGenerationConfig existingCategory = articleGenerationConfigMapper.selectByPrimaryKey(request.getId());
        if (Objects.isNull(existingCategory)) {
            log.warn("删除文章生成配置失败：文章生成配置不存在，id={}", request.getId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 软删除文章生成配置
        int result = articleGenerationConfigMapper.deleteByPrimaryKey(request.getId());
        if (result <= 0) {
            log.error("删除文章生成配置失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public PageRespDto<ArticleGenerationConfigListRespDto> getArticleGenerationConfigList(ArticleGenerationConfigListReqDto request) {
        if (Objects.isNull(request)) {
            log.warn("查询文章生成配置列表失败：请求参数为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 设置分页
        PageHelper.startPage(request.getPageNo(), request.getPageSize());

        // 查询文章生成配置列表
        List<ArticleGenerationConfig> categories = articleGenerationConfigMapper.selectArticleGenerationConfigList(request.getTheme());

        // 获取分页信息
        PageInfo<ArticleGenerationConfig> pageInfo = new PageInfo<>(categories);

        // 转换为列表响应DTO
        List<ArticleGenerationConfigListRespDto> categoryListRespDtos = categories.stream()
                .map(this::convertToListRespDto)
                .toList();

        // 构建分页响应
        PageRespDto<ArticleGenerationConfigListRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setPageNo(pageInfo.getPageNum());
        pageRespDto.setPageSize(pageInfo.getPageSize());
        pageRespDto.setSize(pageInfo.getSize());
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setPages(pageInfo.getPages());
        pageRespDto.setList(categoryListRespDtos);

        return pageRespDto;
    }

    /**
     * 转换为响应DTO
     */
    private ArticleGenerationConfigRespDto convertToRespDto(ArticleGenerationConfig category) {
        ArticleGenerationConfigRespDto dto = new ArticleGenerationConfigRespDto();
        dto.setId(category.getId());
        dto.setTheme(category.getTheme());
        dto.setGender(category.getGender());
        dto.setGenre(category.getGenre());
        dto.setPlot(category.getPlot());
        dto.setCharacterType(category.getCharacterType());
        dto.setStyle(category.getStyle());
        dto.setAdditionalCharacteristics(category.getAdditionalCharacteristics());
        dto.setPendingCount(category.getPendingCount());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        return dto;
    }

    /**
     * 转换为列表响应DTO
     */
    private ArticleGenerationConfigListRespDto convertToListRespDto(ArticleGenerationConfig category) {
        ArticleGenerationConfigListRespDto dto = new ArticleGenerationConfigListRespDto();
        dto.setId(category.getId());
        dto.setTheme(category.getTheme());
        dto.setGender(category.getGender());
        dto.setGenre(category.getGenre());
        dto.setPlot(category.getPlot());
        dto.setCharacterType(category.getCharacterType());
        dto.setStyle(category.getStyle());
        dto.setAdditionalCharacteristics(category.getAdditionalCharacteristics());
        dto.setPendingCount(category.getPendingCount());
        dto.setCreateTime(category.getCreateTime());
        return dto;
    }
}