package com.aicreation.mapper;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.dto.ArticleTitleDedupItemDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章数据访问接口
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Mapper
public interface ArticleMapper {

    /**
     * 根据ID查询文章
     * 
     * @param id 文章ID
     * @return 文章信息
     */
    Article selectByPrimaryKey(@Param("id") Long id);

    /**
     * 根据文章名称查询文章
     * 
     * @param articleName 文章名称
     * @return 文章信息
     */
    Article selectByArticleName(@Param("articleName") String articleName);

    /**
     * 查询文章列表
     *
     * @param articleName 文章名称（可选）
     * @param voiceTone 音色（可选）
     * @param publishStatus 发布状态（可选）
     * @param theme 文章主题/分类（可选）
     * @return 文章列表
     */
    List<Article> selectArticleList(@Param("articleName") String articleName,
                                   @Param("voiceTone") String voiceTone,
                                   @Param("publishStatus") Integer publishStatus,
                                   @Param("theme") String theme,
                                   @Param("scopedCreateUserId") Long scopedCreateUserId);

    /**
     * 插入文章
     * 
     * @param article 文章信息
     * @return 影响行数
     */
    int insert(Article article);

    /**
     * 更新文章
     * 
     * @param article 文章信息
     * @return 影响行数
     */
    int updateByPrimaryKey(Article article);

    /**
     * 根据ID删除文章
     * 
     * @param id 文章ID
     * @return 影响行数
     */
    int deleteByPrimaryKey(@Param("id") Long id);

    /**
     * 更新文章发布状态
     *
     * @param id 文章ID
     * @param publishStatus 发布状态
     * @return 影响行数
     */
    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") Integer publishStatus);

    /**
     * 根据文章类型查询已存在的文章名称列表
     *
     * @param articleType 文章类型
     * @return 文章名称列表
     */
    List<String> selectExistingTitlesByTheme(@Param("theme") String theme);

    /**
     * 根据主题查询已存在文章（标题 + 大纲），用于标题生成去重
     *
     * @param theme 主题
     * @return 已存在文章信息列表
     */
    List<ArticleTitleDedupItemDto> selectExistingTitleDedupItemsByTheme(@Param("theme") String theme);

    /**
     * 查询内容未生成的文章列表
     *
     * @return 文章列表
     */
    List<Article> selectArticlesWithoutContent();

    /**
     * 查询没有章节的文章列表（用于生成章节基本信息）
     *
     * @return 文章列表
     */
    List<Article> selectArticlesWithoutChapters();

    /**
     * 更新文章的response_id
     *
     * @param id 文章ID
     * @param responseId Responses API的response_id
     * @return 影响行数
     */
    int updateResponseId(@Param("id") Long id, @Param("responseId") String responseId);

    /**
     * 根据ID查询文章（带行级锁，用于并发控制）
     *
     * @param id 文章ID
     * @return 文章信息
     */
    Article selectByPrimaryKeyForUpdate(@Param("id") Long id);

    /**
     * 根据文章类型查询文章名称列表
     *
     * @param articleType 文章类型
     * @return 文章名称列表
     */
    List<String> selectArticleNamesByTheme(@Param("theme") String theme);
}
