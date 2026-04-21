package com.aicreation.mapper;

import com.aicreation.entity.po.ArticleGenerationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章生成配置数据访问接口
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Mapper
public interface ArticleGenerationConfigMapper {

    /**
     * 根据ID查询文章生成配置
     *
     * @param id 文章生成配置ID
     * @return 文章生成配置信息
     */
    ArticleGenerationConfig selectByPrimaryKey(@Param("id") Long id);

    /**
     * 根据文章生成配置主题查询文章生成配置
     *
     * @param theme 文章生成配置主题
     * @return 文章生成配置信息
     */
    ArticleGenerationConfig selectByTheme(@Param("theme") String theme);

    /**
     * 根据文章ID查询文章生成配置
     *
     * @param articleId 文章ID
     * @return 文章生成配置信息
     */
    ArticleGenerationConfig selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询文章生成配置列表
     *
     * @param theme 文章生成配置主题（可选）
     * @return 文章生成配置列表
     */
    List<ArticleGenerationConfig> selectArticleGenerationConfigList(@Param("theme") String theme,
                                                                    @Param("scopedCreateUserId") Long scopedCreateUserId);

    /**
     * 插入文章生成配置
     *
     * @param articleGenerationConfig 文章生成配置信息
     * @return 影响行数
     */
    int insert(ArticleGenerationConfig articleGenerationConfig);

    /**
     * 更新文章生成配置
     *
     * @param articleGenerationConfig 文章生成配置信息
     * @return 影响行数
     */
    int updateByPrimaryKey(ArticleGenerationConfig articleGenerationConfig);

    /**
     * 根据ID删除文章生成配置
     *
     * @param id 文章生成配置ID
     * @return 影响行数
     */
    int deleteByPrimaryKey(@Param("id") Long id);

    /**
     * 查询待生成数量大于0的文章生成配置列表
     *
     * @return 文章生成配置列表
     */
    List<ArticleGenerationConfig> selectPendingArticleGenerationConfigs();
}