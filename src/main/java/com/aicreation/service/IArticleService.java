package com.aicreation.service;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;

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
}
