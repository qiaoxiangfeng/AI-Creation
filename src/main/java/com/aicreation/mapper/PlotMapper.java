package com.aicreation.mapper;

import com.aicreation.entity.po.Plot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 伏笔Mapper接口
 */
public interface PlotMapper {

    /**
     * 根据主键查询伏笔
     */
    Plot selectByPrimaryKey(Long id);

    /**
     * 根据文章ID查询所有伏笔
     */
    List<Plot> selectByArticleId(Long articleId);

    /**
     * 根据章节ID查询伏笔
     */
    List<Plot> selectByChapterId(Long chapterId);

    /**
     * 根据回收章节ID查询伏笔
     */
    List<Plot> selectByRecoveryChapterId(Long recoveryChapterId);

    /**
     * 插入伏笔
     */
    int insert(Plot plot);

    /**
     * 根据主键更新伏笔
     */
    int updateByPrimaryKey(Plot plot);

    /**
     * 根据主键删除伏笔（软删除）
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 根据章节ID删除伏笔（软删除）
     */
    int deleteByChapterId(Long chapterId);

    /**
     * 根据文章ID和章节ID查询伏笔
     */
    List<Plot> selectByArticleIdAndChapterId(@Param("articleId") Long articleId, @Param("chapterId") Long chapterId);
}