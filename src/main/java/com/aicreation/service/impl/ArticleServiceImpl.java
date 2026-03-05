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
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.entity.po.Plot;
import java.util.List;
import java.util.ArrayList;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.service.IArticleService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private PlotMapper plotMapper;

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Autowired
    private com.aicreation.task.ArticleContentGenerationTask articleContentGenerationTask;

    @Autowired
    private com.aicreation.task.ArticleChapterGenerationTask articleChapterGenerationTask;

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
        dto.setTotalWordCountEstimate(article.getTotalWordCountEstimate());
        dto.setChapterWordCountEstimate(article.getChapterWordCountEstimate());
        // 处理generationStatus为null的情况，默认为0（未开始）
        dto.setGenerationStatus(article.getGenerationStatus() != null ? article.getGenerationStatus() : 0);
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
        dto.setCorePlot(chapter.getCorePlot());
        dto.setWordCountEstimate(chapter.getWordCountEstimate());
        dto.setChapterVoiceLink(chapter.getChapterVoiceLink());
        dto.setChapterVideoLink(chapter.getChapterVideoLink());

        // 获取章节的伏笔信息
        List<PlotRespDto> plots = getChapterPlots(chapter.getId());
        dto.setPlots(plots);

        return dto;
    }

    /**
     * 获取章节的伏笔信息
     */
    private List<PlotRespDto> getChapterPlots(Long chapterId) {
        if (chapterId == null) {
            return new java.util.ArrayList<>();
        }

        // 查询章节的伏笔信息
        List<Plot> plots = plotMapper.selectByChapterId(chapterId);

        return plots.stream()
                .map(this::convertToPlotRespDto)
                .toList();
    }

    /**
     * 转换Plot为PlotRespDto
     */
    private PlotRespDto convertToPlotRespDto(Plot plot) {
        PlotRespDto dto = new PlotRespDto();
        dto.setId(plot.getId());
        dto.setPlotName(plot.getPlotName());
        dto.setPlotContent(plot.getPlotContent());
        dto.setRecoveryChapterId(plot.getRecoveryChapterId());

        // 如果有回收章节ID，查询对应的章节序号
        if (plot.getRecoveryChapterId() != null) {
            ArticleChapter recoveryChapter = articleChapterMapper.selectByPrimaryKey(plot.getRecoveryChapterId());
            if (recoveryChapter != null) {
                dto.setRecoveryChapterNo(recoveryChapter.getChapterNo());
            }
        }

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

        // 更新文章状态为生成中
        if (article.getGenerationStatus() != 1) {
            article.setGenerationStatus(1); // 1-生成中
            article.setUpdateTime(LocalDateTime.now());
            articleMapper.updateByPrimaryKey(article);
            log.info("文章[{}]状态已更新为生成中", article.getArticleName());
        }

        // 检查是否有章节需要生成内容
        List<ArticleChapter> chaptersWithoutContent = articleChapterMapper.selectChaptersWithoutContentByArticleId(articleId);
        if (chaptersWithoutContent == null || chaptersWithoutContent.isEmpty()) {
            log.info("文章[{}]没有需要生成内容的章节", articleId);
            return true;
        }

        // 异步执行内容生成任务
        try {
            log.info("开始为文章[{}]执行内容生成，共{}个章节待生成内容", articleId, chaptersWithoutContent.size());

            // 直接调用内容生成任务，为该文章的所有待生成章节生成内容
            int processedCount = 0;
            for (ArticleChapter chapter : chaptersWithoutContent) {
                try {
                    // 调用单个章节的内容生成方法
                    articleContentGenerationTask.generateChapterContentManually(chapter);
                    processedCount++;
                    log.info("文章[{}]第{}章内容生成完成", articleId, chapter.getChapterNo());
                } catch (Exception chapterException) {
                    log.error("文章[{}]第{}章内容生成失败：{}", articleId, chapter.getChapterNo(), chapterException.getMessage());
                    // 继续处理其他章节，不因为单个章节失败而停止整个任务
                }
            }

            log.info("文章[{}]内容生成任务完成，共处理{}个章节", articleId, processedCount);

            // 检查是否所有章节都已完成，如果是则更新文章状态
            articleContentGenerationTask.checkAndUpdateArticleCompletionStatusManually(article);

            return true;
        } catch (Exception e) {
            log.error("文章[{}]内容生成失败：{}", articleId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ArticleProgressDto getArticleProgress(Long articleId) {
        if (articleId == null || articleId <= 0) {
            log.warn("获取文章生成进度失败：文章ID无效，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 获取文章信息
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            log.warn("获取文章生成进度失败：文章不存在，articleId={}", articleId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 获取文章的所有章节
        List<ArticleChapter> chapters = articleChapterMapper.selectByArticleId(articleId);

        // 计算进度
        int totalChapters = chapters.size();
        int completedChapters = (int) chapters.stream()
            .filter(chapter -> chapter.getChapterContent() != null && !chapter.getChapterContent().trim().isEmpty())
            .count();

        // 计算字数进度
        int currentWordCount = chapters.stream()
            .filter(chapter -> chapter.getChapterContent() != null)
            .mapToInt(chapter -> chapter.getChapterContent().length())
            .sum();

        // 计算进度百分比
        int progressPercent = totalChapters > 0 ? (completedChapters * 100) / totalChapters : 0;

        // 如果所有章节都有内容，标记为已完成
        Integer currentStatus = article.getGenerationStatus() != null ? article.getGenerationStatus() : 0;
        if (progressPercent == 100 && currentStatus != 2) {
            article.setGenerationStatus(2); // 已完成
            article.setUpdateTime(LocalDateTime.now());
            articleMapper.updateByPrimaryKey(article);
        }

        // 构建进度DTO
        ArticleProgressDto progress = new ArticleProgressDto();
        progress.setArticleId(article.getId());
        progress.setArticleName(article.getArticleName());
        progress.setGenerationStatus(article.getGenerationStatus());
        progress.setTotalChapters(totalChapters);
        progress.setCompletedChapters(completedChapters);
        progress.setProgressPercent(progressPercent);
        progress.setTotalWordCountEstimate(article.getTotalWordCountEstimate());
        progress.setCurrentWordCount(currentWordCount);
        progress.setCreateTime(article.getCreateTime().toString());
        progress.setUpdateTime(article.getUpdateTime().toString());

        return progress;
    }

    @Override
    public Boolean deleteChapter(Long chapterId) {
        log.info("开始删除章节，chapterId={}", chapterId);
        try {
            // 首先删除章节关联的伏笔信息
            plotMapper.deleteByChapterId(chapterId);

            // 删除章节本身
            articleChapterMapper.deleteByPrimaryKey(chapterId);

            log.info("章节删除成功，chapterId={}", chapterId);
            return true;
        } catch (Exception e) {
            log.error("删除章节失败，chapterId={}：{}", chapterId, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public Boolean generateArticleChapters(Long articleId) {
        log.info("开始为文章{}生成章节", articleId);
        try {
            // 获取文章信息
            Article article = articleMapper.selectByPrimaryKey(articleId);
            if (article == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "文章不存在");
            }

            // 检查文章状态
            if (article.getGenerationStatus() == 1) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "文章正在生成中，请等待完成后再操作");
            }

            // 异步调用章节生成任务
            articleChapterGenerationTask.generateChaptersForArticle(article);

            log.info("文章{}章节生成任务已启动", articleId);
            return true;
        } catch (Exception e) {
            log.error("启动文章{}章节生成失败：{}", articleId, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public Boolean generateArticleChapterContent(Long articleId) {
        log.info("开始为文章{}生成章节内容", articleId);
        try {
            // 获取文章信息
            Article article = articleMapper.selectByPrimaryKey(articleId);
            if (article == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "文章不存在");
            }

            // 检查文章状态
            if (article.getGenerationStatus() == 1) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "文章正在生成中，请等待完成后再操作");
            }

            // 获取文章的所有章节
            List<ArticleChapter> chapters = articleChapterMapper.selectByArticleId(articleId);
            // 按章节号排序
            chapters.sort((a, b) -> Integer.compare(a.getChapterNo(), b.getChapterNo()));

            if (chapters.isEmpty()) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "文章没有章节信息，请先生成章节");
            }

            // 异步调用内容生成任务
            articleContentGenerationTask.generateContentsForArticle(article, chapters);

            log.info("文章{}章节内容生成任务已启动", articleId);
            return true;
        } catch (Exception e) {
            log.error("启动文章{}章节内容生成失败：{}", articleId, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR);
        }
    }

    @Override
    public Boolean updateChapterInfo(Long chapterId, String corePlot, Integer wordCountEstimate, List<PlotReqDto> plots) {
        if (chapterId == null || chapterId <= 0) {
            log.warn("更新章节信息失败：章节ID无效，chapterId={}", chapterId);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 获取章节信息
        ArticleChapter chapter = articleChapterMapper.selectByPrimaryKey(chapterId);
        if (chapter == null) {
            log.warn("更新章节信息失败：章节不存在，chapterId={}", chapterId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 更新章节信息
        boolean chapterUpdated = false;
        if (StringUtils.hasText(corePlot) || wordCountEstimate != null) {
            if (StringUtils.hasText(corePlot)) {
                chapter.setCorePlot(corePlot);
            }
            if (wordCountEstimate != null) {
                chapter.setWordCountEstimate(wordCountEstimate);
            }
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);
            chapterUpdated = true;
            log.info("更新章节基本信息成功，chapterId={}", chapterId);
        }

        // 处理伏笔信息
        if (plots != null) {
            // 先删除现有的伏笔
            plotMapper.deleteByChapterId(chapterId);

            // 添加新的伏笔
            for (PlotReqDto plotReq : plots) {
                Plot plot = new Plot();
                plot.setArticleId(chapter.getArticleId());
                plot.setChapterId(chapterId);
                plot.setPlotName(plotReq.getPlotName());
                plot.setPlotContent(plotReq.getPlotContent());
                plot.setRecoveryChapterId(plotReq.getRecoveryChapterId());
                plot.setResState(1);
                plot.setCreateTime(LocalDateTime.now());
                plot.setUpdateTime(LocalDateTime.now());
                plotMapper.insert(plot);
            }
            log.info("更新章节伏笔信息成功，chapterId={}，伏笔数量={}", chapterId, plots.size());
        }

        return true;
    }











}
