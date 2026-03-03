package com.aicreation.service;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;

/**
 * 文章生成配置服务接口
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public interface IArticleGenerationConfigService {

    /**
     * 根据ID查询文章生成配置信息
     *
     * @param request 查询请求
     * @return 文章生成配置信息，如果不存在返回null
     */
    ArticleGenerationConfigRespDto getArticleGenerationConfigById(ArticleGenerationConfigQueryReqDto request);

    /**
     * 根据文章生成配置主题查询文章生成配置信息
     *
     * @param theme 文章生成配置主题
     * @return 文章生成配置信息，如果不存在返回null
     */
    ArticleGenerationConfigRespDto getArticleGenerationConfigByTheme(String theme);

    /**
     * 创建文章生成配置
     *
     * @param request 创建请求
     * @return 创建后的文章生成配置ID
     */
    Long createArticleGenerationConfig(ArticleGenerationConfigCreateReqDto request);

    /**
     * 更新文章生成配置信息
     *
     * @param request 更新请求
     * @return 是否更新成功
     */
    Boolean updateArticleGenerationConfig(ArticleGenerationConfigUpdateReqDto request);

    /**
     * 删除文章生成配置
     *
     * @param request 删除请求
     * @return 是否删除成功
     */
    Boolean deleteArticleGenerationConfig(ArticleGenerationConfigDeleteReqDto request);

    /**
     * 查询文章生成配置列表（分页）
     *
     * @param request 查询请求
     * @return 文章生成配置列表
     */
    PageRespDto<ArticleGenerationConfigListRespDto> getArticleGenerationConfigList(ArticleGenerationConfigListReqDto request);
}