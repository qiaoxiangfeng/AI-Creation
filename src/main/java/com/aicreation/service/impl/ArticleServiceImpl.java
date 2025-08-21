package com.aicreation.service.impl;

import com.aicreation.common.ArticleStatus;
import com.aicreation.common.BusinessException;
import com.aicreation.common.ErrorCode;
import com.aicreation.converter.ArticleConverter;
import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.Article;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.service.IArticleService;
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
 * 文章服务实现类
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Service
public class ArticleServiceImpl implements IArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleConverter articleConverter;

    @Override
    public ArticleRespDto getArticleById(ArticleQueryReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("查询文章失败：文章ID为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Article article = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(article)) {
            return null;
        }

        return articleConverter.toArticleRespDto(article);
    }

    @Override
    public ArticleRespDto getArticleByArticleName(String articleName) {
        if (!StringUtils.hasText(articleName)) {
            log.warn("查询文章失败：文章名称为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Article article = articleMapper.selectByArticleName(articleName);
        if (Objects.isNull(article)) {
            return null;
        }

        return articleConverter.toArticleRespDto(article);
    }

    @Override
    public Long createArticle(ArticleCreateReqDto request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getArticleName())) {
            log.warn("创建文章失败：请求参数无效");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 检查文章名称是否已存在
        Article existingArticle = articleMapper.selectByArticleName(request.getArticleName());
        if (Objects.nonNull(existingArticle)) {
            log.warn("创建文章失败：文章名称已存在，articleName={}", request.getArticleName());
            throw new BusinessException(ErrorCode.DUPLICATE_DATA);
        }

        // 转换为业务对象
        ArticleBo articleBo = articleConverter.toArticleBo(request);
        articleBo.setResState(1);
        articleBo.setPublishStatus(ArticleStatus.UNPUBLISHED); // 默认未发布
        articleBo.setCreateTime(LocalDateTime.now());
        articleBo.setUpdateTime(LocalDateTime.now());

        // 转换为持久化对象
        Article article = articleConverter.toArticle(articleBo);

        // 保存到数据库
        int result = articleMapper.insert(article);
        if (result <= 0) {
            log.error("创建文章失败：数据库插入失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return article.getId();
    }

    @Override
    public Boolean updateArticle(ArticleUpdateReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("更新文章失败：请求参数无效");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(existingArticle)) {
            log.warn("更新文章失败：文章不存在，articleId={}", request.getArticleId());
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        // 如果更新文章名称，检查是否与其他文章重复
        if (StringUtils.hasText(request.getArticleName()) && 
            !request.getArticleName().equals(existingArticle.getArticleName())) {
            Article duplicateArticle = articleMapper.selectByArticleName(request.getArticleName());
            if (Objects.nonNull(duplicateArticle)) {
                log.warn("更新文章失败：文章名称已存在，articleName={}", request.getArticleName());
                throw new BusinessException(ErrorCode.DUPLICATE_DATA);
            }
        }

        // 转换为持久化对象
        Article article = articleConverter.toArticle(request);
        article.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        int result = articleMapper.updateByPrimaryKey(article);
        if (result <= 0) {
            log.error("更新文章失败：数据库更新失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public Boolean deleteArticle(ArticleDeleteReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("删除文章失败：请求参数无效");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(existingArticle)) {
            log.warn("删除文章失败：文章不存在，articleId={}", request.getArticleId());
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        // 软删除文章
        int result = articleMapper.deleteByPrimaryKey(request.getArticleId());
        if (result <= 0) {
            log.error("删除文章失败：数据库更新失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public PageRespDto<ArticleListRespDto> getArticleList(ArticleListReqDto request) {
        if (Objects.isNull(request)) {
            log.warn("查询文章列表失败：请求参数为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 设置分页
        PageHelper.startPage(request.getPageNo(), request.getPageSize());

        // 查询文章列表
        List<Article> articles = articleMapper.selectArticleList(
            request.getArticleName(), 
            request.getVoiceTone(),
            request.getPublishStatus()
        );

        // 获取分页信息
        PageInfo<Article> pageInfo = new PageInfo<>(articles);

        // 转换为列表响应DTO（不包含删除状态）
        List<ArticleListRespDto> articleListRespDtos = articles.stream()
            .map(this::convertToArticleListRespDto)
            .toList();

        // 构建分页响应
        PageRespDto<ArticleListRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setPageNo(pageInfo.getPageNum());
        pageRespDto.setPageSize(pageInfo.getPageSize());
        pageRespDto.setSize(pageInfo.getSize());
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setPages(pageInfo.getPages());
        pageRespDto.setList(articleListRespDtos);

        return pageRespDto;
    }

    /**
     * 将Article实体转换为ArticleListRespDto（不包含删除状态）
     * 
     * @param article 文章实体
     * @return 文章列表响应DTO
     */
    private ArticleListRespDto convertToArticleListRespDto(Article article) {
        ArticleListRespDto dto = new ArticleListRespDto();
        dto.setId(article.getId());
        dto.setArticleName(article.getArticleName());
        dto.setArticleOutline(article.getArticleOutline());
        dto.setArticleContent(article.getArticleContent());
        dto.setVoiceTone(article.getVoiceTone());
        dto.setVoiceLink(article.getVoiceLink());
        dto.setVoiceFilePath(article.getVoiceFilePath());
        dto.setVideoLink(article.getVideoLink());
        dto.setVideoFilePath(article.getVideoFilePath());
        dto.setPublishStatus(article.getPublishStatus());
        dto.setCreateTime(article.getCreateTime());
        dto.setUpdateTime(article.getUpdateTime());
        return dto;
    }

    @Override
    public Boolean updateArticlePublishStatus(Long articleId, Integer publishStatus) {
        if (Objects.isNull(articleId) || Objects.isNull(publishStatus)) {
            log.warn("更新文章发布状态失败：参数无效");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 验证发布状态值
        if (!ArticleStatus.isValidStatus(publishStatus)) {
            log.warn("更新文章发布状态失败：发布状态值无效，publishStatus={}", publishStatus);
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(articleId);
        if (Objects.isNull(existingArticle)) {
            log.warn("更新文章发布状态失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        // 更新发布状态
        int result = articleMapper.updatePublishStatus(articleId, publishStatus);
        if (result <= 0) {
            log.error("更新文章发布状态失败：数据库更新失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return true;
    }
}
