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
}
