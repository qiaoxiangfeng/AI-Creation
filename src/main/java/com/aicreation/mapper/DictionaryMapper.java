package com.aicreation.mapper;

import com.aicreation.entity.po.Dictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据访问接口
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Mapper
public interface DictionaryMapper {

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典信息
     */
    Dictionary selectByPrimaryKey(@Param("id") Long id);

    /**
     * 根据字典键查询字典列表
     *
     * @param dictKey 字典键
     * @return 字典列表
     */
    List<Dictionary> selectByDictKey(@Param("dictKey") String dictKey);

    /**
     * 查询所有字典
     *
     * @return 字典列表
     */
    List<Dictionary> selectAll();

    /**
     * 查询字典列表
     *
     * @param dictKey 字典键（可选）
     * @param dictValue 字典值（可选）
     * @return 字典列表
     */
    List<Dictionary> selectDictionaryList(@Param("dictKey") String dictKey, @Param("dictValue") String dictValue);

    /**
     * 插入字典
     *
     * @param dictionary 字典信息
     * @return 影响行数
     */
    int insert(Dictionary dictionary);

    /**
     * 更新字典
     *
     * @param dictionary 字典信息
     * @return 影响行数
     */
    int updateByPrimaryKey(Dictionary dictionary);

    /**
     * 根据ID删除字典
     *
     * @param id 字典ID
     * @return 影响行数
     */
    int deleteByPrimaryKey(@Param("id") Long id);
}