package com.aicreation.service;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.User;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public interface IUserService {

    /**
     * 根据用户ID查询用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息，如果不存在返回null
     * @throws IllegalArgumentException 当userId为null时抛出
     */
    UserRespDto getUserById(UserQueryReqDto request);

    /**
     * 根据用户名查询用户信息
     * 
     * @param userName 用户名
     * @return 用户信息，如果不存在返回null
     */
    User getUserByUserName(String userName);

    /**
     * 创建用户
     * 
     * @param userDto 用户DTO
     * @return 创建后的用户ID
     */
    Long createUser(UserCreateReqDto request);

    /**
     * 更新用户信息
     * 
     * @param userDto 用户DTO
     * @return 是否更新成功
     */
    Boolean updateUser(UserUpdateReqDto request);

    /**
     * 删除用户
     * 
     * @param userId 用户ID
     * @return 是否删除成功
     */
    Boolean deleteUser(UserDeleteReqDto request);

    /**
     * 查询用户列表（分页）
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param userName 用户名搜索关键词（可选）
     * @return 用户列表
     */
    PageRespDto<UserRespDto> getUserList(UserListReqDto request);

    /**
     * 用户登录
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 登录成功返回用户信息，失败返回null
     */
    UserRespDto login(UserLoginReqDto request);

    /**
     * 初始化用户密码
     * 
     * @param userIds 用户ID列表，为空时初始化所有用户密码
     * @param newPassword 新密码，为空时使用默认密码"123456"
     * @return 初始化成功的用户数量
     */
    Integer initializeUserPasswords(UserPasswordInitReqDto request);
} 