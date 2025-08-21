package com.aicreation.mapper;

import com.aicreation.entity.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 用户Mapper接口
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Mapper
public interface UserMapper {

    /**
     * 根据主键查询用户
     * 
     * @param id 主键ID
     * @return 用户信息
     */
    User selectByPrimaryKey(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户信息
     */
    User selectByUserName(@Param("userName") String userName);

    /**
     * 插入用户
     * 
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 根据主键更新用户
     * 
     * @param user 用户信息
     * @return 影响行数
     */
    int updateByPrimaryKeySelective(User user);

    /**
     * 根据主键删除用户
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteByPrimaryKey(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 查询用户列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param userName 用户名搜索关键词（可选）
     * @return 用户列表
     */
    List<User> selectUserList(@Param("pageNum") Integer pageNum, @Param("pageSize") Integer pageSize, @Param("userName") String userName);

    /**
     * 批量更新用户密码
     * 
     * @param userIds 用户ID列表，为空时更新所有用户
     * @param newPassword 新密码（已加密）
     * @return 影响行数
     */
    int updateUserPasswords(@Param("userIds") List<Long> userIds,
                            @Param("newPassword") String newPassword,
                            @Param("updateTime") LocalDateTime updateTime);
} 