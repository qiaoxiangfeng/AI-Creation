package com.aicreation.mapper;

import com.aicreation.entity.po.Article;
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
     * @return 文章列表
     */
    List<Article> selectArticleList(@Param("articleName") String articleName, 
                                   @Param("voiceTone") String voiceTone,
                                   @Param("publishStatus") Integer publishStatus);

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
}
