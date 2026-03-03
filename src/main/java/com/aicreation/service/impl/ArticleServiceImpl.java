package com.aicreation.service.impl;

import com.aicreation.enums.ArticleStatusEnum;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.converter.ArticleConverter;
import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import java.util.List;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.ArticleChapterMapper;
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
    private ArticleChapterMapper articleChapterMapper;

    @Override
    public ArticleRespDto getArticleById(ArticleQueryReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("查询文章失败：文章ID为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Article article = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(article)) {
            return null;
        }

        return ArticleConverter.INSTANCE.toArticleRespDto(article);
    }

    @Override
    public ArticleRespDto getArticleByArticleName(String articleName) {
        if (!StringUtils.hasText(articleName)) {
            log.warn("查询文章失败：文章名称为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Article article = articleMapper.selectByArticleName(articleName);
        if (Objects.isNull(article)) {
            return null;
        }

        return ArticleConverter.INSTANCE.toArticleRespDto(article);
    }

    @Override
    public Long createArticle(ArticleCreateReqDto request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getArticleName())) {
            log.warn("创建文章失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章名称是否已存在
        Article existingArticle = articleMapper.selectByArticleName(request.getArticleName());
        if (Objects.nonNull(existingArticle)) {
            log.warn("创建文章失败：文章名称已存在，articleName={}", request.getArticleName());
            throw new BusinessException(ErrorCodeEnum.DUPLICATE_DATA);
        }

        // 转换为业务对象
        ArticleBo articleBo = ArticleConverter.INSTANCE.toArticleBo(request);
        articleBo.setResState(1);
        articleBo.setPublishStatus(ArticleStatusEnum.UNPUBLISHED.getCode()); // 默认未发布
        articleBo.setCreateTime(LocalDateTime.now());
        articleBo.setUpdateTime(LocalDateTime.now());

        // 转换为持久化对象
        Article article = ArticleConverter.INSTANCE.toArticle(articleBo);

        // 保存到数据库
        int result = articleMapper.insert(article);
        if (result <= 0) {
            log.error("创建文章失败：数据库插入失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return article.getId();
    }

    @Override
    public Boolean updateArticle(ArticleUpdateReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("更新文章失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(existingArticle)) {
            log.warn("更新文章失败：文章不存在，articleId={}", request.getArticleId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 如果更新文章名称，检查是否与其他文章重复
        if (StringUtils.hasText(request.getArticleName()) && 
            !request.getArticleName().equals(existingArticle.getArticleName())) {
            Article duplicateArticle = articleMapper.selectByArticleName(request.getArticleName());
            if (Objects.nonNull(duplicateArticle)) {
                log.warn("更新文章失败：文章名称已存在，articleName={}", request.getArticleName());
                throw new BusinessException(ErrorCodeEnum.DUPLICATE_DATA);
            }
        }

        // 转换为持久化对象
        Article article = ArticleConverter.INSTANCE.toArticle(request);
        article.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        int result = articleMapper.updateByPrimaryKey(article);
        if (result <= 0) {
            log.error("更新文章失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public Boolean deleteArticle(ArticleDeleteReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getArticleId())) {
            log.warn("删除文章失败：请求参数无效");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(request.getArticleId());
        if (Objects.isNull(existingArticle)) {
            log.warn("删除文章失败：文章不存在，articleId={}", request.getArticleId());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 软删除文章
        int result = articleMapper.deleteByPrimaryKey(request.getArticleId());
        if (result <= 0) {
            log.error("删除文章失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public PageRespDto<ArticleListRespDto> getArticleList(ArticleListReqDto request) {
        if (Objects.isNull(request)) {
            log.warn("查询文章列表失败：请求参数为空");
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }




        // 设置分页
        PageHelper.startPage(request.getPageNo(), request.getPageSize());

        // 查询文章列表
        List<Article> articles = articleMapper.selectArticleList(
            request.getArticleName(),
            request.getVoiceTone(),
            request.getPublishStatus(),
            request.getArticleType()
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
        dto.setArticleType(article.getArticleType());
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
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 验证发布状态值
        if (!ArticleStatusEnum.isValidStatus(publishStatus)) {
            log.warn("更新文章发布状态失败：发布状态值无效，publishStatus={}", publishStatus);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article existingArticle = articleMapper.selectByPrimaryKey(articleId);
        if (Objects.isNull(existingArticle)) {
            log.warn("更新文章发布状态失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 更新发布状态
        int result = articleMapper.updatePublishStatus(articleId, publishStatus);
        if (result <= 0) {
            log.error("更新文章发布状态失败：数据库更新失败");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return true;
    }

    @Override
    public List<ArticleChapterRespDto> getArticleChapters(Long articleId) {
        if (articleId == null || articleId <= 0) {
            log.warn("查询文章章节失败：文章ID无效，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 查询文章是否存在
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            log.warn("查询文章章节失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 查询文章章节列表
        List<ArticleChapter> chapters = articleChapterMapper.selectByArticleId(articleId);

        // 转换为响应DTO
        return chapters.stream()
                .map(this::convertToChapterRespDto)
                .toList();
    }

    /**
     * 转换为章节响应DTO
     */
    private ArticleChapterRespDto convertToChapterRespDto(ArticleChapter chapter) {
        ArticleChapterRespDto dto = new ArticleChapterRespDto();
        dto.setId(chapter.getId());
        dto.setChapterNo(chapter.getChapterNo());
        dto.setChapterTitle(chapter.getChapterTitle());
        dto.setChapterContent(chapter.getChapterContent());
        dto.setChapterVoiceLink(chapter.getChapterVoiceLink());
        dto.setChapterVideoLink(chapter.getChapterVideoLink());
        return dto;
    }

    @Override
    public String getArticleFullText(Long articleId) {
        if (articleId == null || articleId <= 0) {
            log.warn("获取文章完整文本失败：文章ID无效，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 获取文章信息
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            log.warn("获取文章完整文本失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 构建完整文本
        StringBuilder fullText = new StringBuilder();

        // 添加文章标题
        fullText.append(article.getArticleName()).append("\n");
        fullText.append("=".repeat(Math.max(0, article.getArticleName().length()))).append("\n\n");

        // 添加文章简介
        if (StringUtils.hasText(article.getArticleOutline())) {
            fullText.append("【故事大纲】\n");
            fullText.append(article.getArticleOutline()).append("\n\n");
        }

        // 获取并添加所有章节内容
        List<ArticleChapter> chapters = articleChapterMapper.selectByArticleId(articleId);
        if (chapters != null && !chapters.isEmpty()) {
            // 按章节序号排序
            chapters.sort((a, b) -> Integer.compare(a.getChapterNo(), b.getChapterNo()));

            for (ArticleChapter chapter : chapters) {
                fullText.append("第").append(chapter.getChapterNo()).append("章 ");
                fullText.append(chapter.getChapterTitle()).append("\n");
                fullText.append("-".repeat(Math.max(0, ("第" + chapter.getChapterNo() + "章 " + chapter.getChapterTitle()).length()))).append("\n\n");

                if (StringUtils.hasText(chapter.getChapterContent())) {
                    fullText.append(chapter.getChapterContent()).append("\n\n");
                } else {
                    fullText.append("【章节内容暂未生成】\n\n");
                }
            }
        } else {
            fullText.append("【暂无章节内容】\n");
        }

        // 添加生成信息
        fullText.append("\n---\n");
        fullText.append("本文由AI生成，仅供娱乐。\n");
        fullText.append("生成时间：").append(java.time.LocalDateTime.now().toString()).append("\n");

        return fullText.toString();
    }

    @Override
    public boolean generateArticleContent(Long articleId) {
        if (articleId == null || articleId <= 0) {
            log.warn("触发文章内容生成失败：文章ID无效，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 检查文章是否存在
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            log.warn("触发文章内容生成失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 检查参数是否设置完整
        if (article.getTotalWordCountEstimate() == null || article.getChapterWordCountEstimate() == null) {
            log.warn("触发文章内容生成失败：文章字数预估参数未设置完整，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "请先设置文章的总字数预估和每章节字数预估");
        }

        // 检查是否有章节需要生成内容
        List<ArticleChapter> chaptersWithoutContent = articleChapterMapper.selectChaptersWithoutContentByArticleId(articleId);
        if (chaptersWithoutContent == null || chaptersWithoutContent.isEmpty()) {
            log.info("文章[{}]没有需要生成内容的章节", articleId);
            return true;
        }

        // 异步执行内容生成任务
        try {
            // 这里可以直接调用内容生成逻辑，或者通过事件发布机制
            // 为简化实现，我们可以记录日志表示任务已启动
            // 实际的内容生成仍然通过定时任务执行

            log.info("成功触发文章[{}]的内容生成，共{}个章节待生成内容", articleId, chaptersWithoutContent.size());
            log.info("内容生成将通过定时任务自动执行，请稍后查看结果");

            return true;
        } catch (Exception e) {
            log.error("触发文章内容生成失败：{}", e.getMessage(), e);
            return false;
        }
    }
}
