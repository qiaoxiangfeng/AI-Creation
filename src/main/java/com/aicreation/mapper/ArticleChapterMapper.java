package com.aicreation.mapper;

import com.aicreation.entity.po.ArticleChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章章节 Mapper
 */
@Mapper
public interface ArticleChapterMapper {

    ArticleChapter selectByPrimaryKey(@Param("id") Long id);

    List<ArticleChapter> selectByArticleId(@Param("articleId") Long articleId);

    ArticleChapter selectByArticleIdAndChapterNo(@Param("articleId") Long articleId, @Param("chapterNo") Integer chapterNo);

    int insert(ArticleChapter record);

    int updateByPrimaryKey(ArticleChapter record);

    int deleteByPrimaryKey(@Param("id") Long id);

    /**
     * 查询没有内容的章节列表（用于内容生成）
     */
    List<ArticleChapter> selectChaptersWithoutContent();

    /**
     * 根据文章ID查询没有内容的章节列表
     */
    List<ArticleChapter> selectChaptersWithoutContentByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询没有内容的章节，排除文章状态为生成中的章节
     */
    List<ArticleChapter> selectChaptersWithoutContentExcludingGenerating();

    /**
     * 根据生成状态查询章节列表
     *
     * @param generationStatus 生成状态
     * @return 章节列表
     */
    List<ArticleChapter> selectChaptersByGenerationStatus(@Param("generationStatus") Integer generationStatus);
}
