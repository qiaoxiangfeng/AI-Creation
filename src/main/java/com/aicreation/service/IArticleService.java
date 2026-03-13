package com.aicreation.service;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import java.util.List;

/**
 * 文章服务接口
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public interface IArticleService {

    /**
     * 根据文章ID查询文章信息
     * 
     * @param request 查询请求
     * @return 文章信息，如果不存在返回null
     */
    ArticleRespDto getArticleById(ArticleQueryReqDto request);

    /**
     * 根据文章名称查询文章信息
     * 
     * @param articleName 文章名称
     * @return 文章信息，如果不存在返回null
     */
    ArticleRespDto getArticleByArticleName(String articleName);

    /**
     * 创建文章
     * 
     * @param request 创建请求
     * @return 创建后的文章ID
     */
    Long createArticle(ArticleCreateReqDto request);

    /**
     * 创建文章并设置 response_id（用于 Responses API 上下文关联）
     *
     * @param request 创建请求
     * @param responseId Responses API 的 response_id，可为空
     * @return 创建后的文章ID
     */
    Long createArticleWithResponseId(ArticleCreateReqDto request, String responseId);

    /**
     * 更新文章信息
     * 
     * @param request 更新请求
     * @return 是否更新成功
     */
    Boolean updateArticle(ArticleUpdateReqDto request);

    /**
     * 删除文章
     * 
     * @param request 删除请求
     * @return 是否删除成功
     */
    Boolean deleteArticle(ArticleDeleteReqDto request);

    /**
     * 查询文章列表（分页）
     *
     * @param request 查询请求
     * @return 文章列表
     */
    PageRespDto<ArticleListRespDto> getArticleList(ArticleListReqDto request);

    /**
     * 更新文章发布状态
     *
     * @param articleId 文章ID
     * @param publishStatus 发布状态
     * @return 是否更新成功
     */
    Boolean updateArticlePublishStatus(Long articleId, Integer publishStatus);

    /**
     * 获取文章章节列表
     *
     * @param articleId 文章ID
     * @return 章节列表
     */
    List<ArticleChapterRespDto> getArticleChapters(Long articleId);

    /**
     * 获取文章完整文本
     *
     * @param articleId 文章ID
     * @return 包含所有章节的完整文本
     */
    String getArticleFullText(Long articleId);

    /**
     * 触发文章内容生成
     *
     * @param articleId 文章ID
     * @return 是否成功启动生成任务
     */
    boolean generateArticleContent(Long articleId);

    /**
     * 获取文章生成进度
     *
     * @param articleId 文章ID
     * @return 文章生成进度信息
     */
    ArticleProgressDto getArticleProgress(Long articleId);

    /**
     * 更新章节信息（核心剧情、字数预估、伏笔）
     *
     * @param chapterId 章节ID
     * @param corePlot 核心剧情
     * @param wordCountEstimate 字数预估
     * @param plots 伏笔列表
     * @return 是否更新成功
     */
    Boolean updateChapterInfo(Long chapterId, String corePlot, Integer wordCountEstimate, List<PlotReqDto> plots);


    /**
     * 删除章节
     * @param chapterId 章节ID
     * @return 是否删除成功
     */
    Boolean deleteChapter(Long chapterId);

    /**
     * 生成文章章节
     * @param articleId 文章ID
     * @return 是否启动成功
     */
    Boolean generateArticleChapters(Long articleId);

    /**
     * 生成文章章节内容
     * @param articleId 文章ID
     * @return 是否启动成功
     */
    Boolean generateArticleChapterContent(Long articleId);
}
