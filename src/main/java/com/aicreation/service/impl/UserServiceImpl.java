package com.aicreation.service.impl;

import com.aicreation.common.BusinessException;
import com.aicreation.common.ErrorCode;
import com.aicreation.converter.UserConverter;
import com.aicreation.entity.bo.UserBo;
import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.User;
import com.aicreation.mapper.UserMapper;
import com.aicreation.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.Objects;

import java.time.LocalDateTime;
import java.util.List;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 用户服务实现类
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserConverter userConverter;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserRespDto getUserById(UserQueryReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getUserId())) {
            log.warn("参数错误：用户ID为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        Long userId = request.getUserId();
        log.debug("根据用户ID查询用户信息，用户ID: {}", userId);
        User user = userMapper.selectByPrimaryKey(userId);
        
        if (Objects.isNull(user)) {
            log.warn("用户不存在，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        log.info("查询用户成功，用户ID: {}, 用户名: {}", userId, user.getUserName());
        return userConverter.toUserRespDto(user);
    }

    @Override
    public User getUserByUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            log.warn("用户名为空");
            return null;
        }
        
        log.debug("根据用户名查询用户信息，用户名: {}", userName);
        User user = userMapper.selectByUserName(userName);
        
        if (user == null) {
            log.warn("用户不存在，用户名: {}", userName);
            return null;
        }
        
        log.info("查询用户成功，用户名: {}", userName);
        return user;
    }

    @Override
    @Transactional
    public Long createUser(UserCreateReqDto request) {
        if (Objects.isNull(request)) {
            log.warn("参数错误：创建用户请求为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        if (!StringUtils.hasText(request.getUserName())) {
            log.warn("参数错误：用户名为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        // 创建用户时密码必填
        if (!StringUtils.hasText(request.getUserPassword())) {
            log.warn("参数错误：密码为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        // 检查用户名是否已存在
        User existingUser = getUserByUserName(request.getUserName());
        if (Objects.nonNull(existingUser)) {
            log.warn("用户已存在，用户名:{}", request.getUserName());
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        // DTO -> BO 转换
        UserBo userBo = userConverter.toUserBo(request);

        // 创建用户实体
        User user = new User();
        user.setUserName(userBo.getUserName());
        // 对密码进行BCrypt加密
        user.setUserPassword(passwordEncoder.encode(userBo.getUserPassword()));
        
        // 可选字段，只有在有值时才设置
        if (StringUtils.hasText(userBo.getUserEmail())) {
            user.setUserEmail(userBo.getUserEmail());
        }
        if (StringUtils.hasText(userBo.getUserPhone())) {
            user.setUserPhone(userBo.getUserPhone());
        }
        
        user.setUserStatus(1); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setResState(1); // 默认有效
        
        log.info("创建用户，用户名: {}", request.getUserName());
        int result = userMapper.insert(user);
        
        if (result > 0) {
            log.info("用户创建成功，用户ID: {}", user.getId());
            return user.getId();
        } else {
            log.error("用户创建失败，用户名: {}", request.getUserName());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional
    public Boolean updateUser(UserUpdateReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getUserId())) {
            log.warn("参数错误：更新请求或用户ID为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        // 检查用户是否存在
        User existingUser = userMapper.selectByPrimaryKey(request.getUserId());
        if (Objects.isNull(existingUser)) {
            log.warn("更新失败，用户不存在，用户ID:{}", request.getUserId());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // DTO -> BO 转换
        UserBo userBo = userConverter.toUserBo(request);

        // 更新用户信息
        User user = new User();
        user.setId(userBo.getId());
        user.setUserName(userBo.getUserName());
        
        // 可选字段，只有在有值时才更新
        if (StringUtils.hasText(userBo.getUserEmail())) {
            user.setUserEmail(userBo.getUserEmail());
        }
        if (StringUtils.hasText(userBo.getUserPhone())) {
            user.setUserPhone(userBo.getUserPhone());
        }
        
        // 处理密码更新（可选）
        if (StringUtils.hasText(userBo.getUserPassword())) {
            // 对密码进行加密处理
            String encryptedPassword = passwordEncoder.encode(userBo.getUserPassword());
            user.setUserPassword(encryptedPassword);
        }
        
        user.setUpdateTime(LocalDateTime.now());
        
        log.info("更新用户信息，用户ID: {}", request.getUserId());
        int result = userMapper.updateByPrimaryKeySelective(user);
        
        if (result <= 0) {
            log.error("用户信息更新失败，用户ID: {}", request.getUserId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        log.info("用户信息更新成功，用户ID: {}", request.getUserId());
        return true;
    }

    @Override
    @Transactional
    public Boolean deleteUser(UserDeleteReqDto request) {
        if (Objects.isNull(request) || Objects.isNull(request.getUserId())) {
            log.warn("参数错误：删除请求或用户ID为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        // 检查用户是否存在
        Long userId = request.getUserId();
        User existingUser = userMapper.selectByPrimaryKey(userId);
        if (Objects.isNull(existingUser)) {
            log.warn("删除失败，用户不存在，用户ID:{}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        log.info("删除用户，用户ID: {}", userId);
        int result = userMapper.deleteByPrimaryKey(userId, LocalDateTime.now());
        
        if (result <= 0) {
            log.error("用户删除失败，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        log.info("用户删除成功，用户ID: {}", userId);
        return true;
    }

    @Override
    public PageRespDto<UserRespDto> getUserList(UserListReqDto request) {
        // 使用PageReqDto的验证方法获取分页参数
        Integer pageNum = request.getValidatedPageNo();
        Integer pageSize = request.getValidatedPageSize();
        String userName = request.getUserName();
        
        log.debug("查询用户列表，页码: {}, 每页大小: {}, 用户名搜索: {}", pageNum, pageSize, userName);
        PageHelper.startPage(pageNum, pageSize);
        List<User> userList = userMapper.selectUserList(pageNum, pageSize, userName);
        
        log.info("查询用户列表成功，总数: {}", userList.size());
        PageInfo<User> pageInfo = new PageInfo<>(userList);
        return PageRespDto.of(pageInfo).convert(userConverter::toUserRespDto);
    }

    @Override
    public UserRespDto login(UserLoginReqDto request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getUserName()) || !StringUtils.hasText(request.getPassword())) {
            log.warn("用户名或密码为空");
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        log.debug("用户登录，用户名: {}", request.getUserName());
        
        // 根据用户名查询用户
        User user = getUserByUserName(request.getUserName());
        if (Objects.isNull(user)) {
            log.warn("用户不存在，用户名: {}", request.getUserName());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        
        // 验证密码（使用BCrypt验证）
        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            log.warn("密码错误，用户名: {}", request.getUserName());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(user);
        
        log.info("用户登录成功，用户名: {}", request.getUserName());
        return userConverter.toUserRespDto(user);
    }

    @Override
    @Transactional
    public Integer initializeUserPasswords(UserPasswordInitReqDto request) {
        // 设置默认密码
        String newPassword = request.getNewPassword();
        if (!StringUtils.hasText(newPassword)) {
            newPassword = "123456";
        }
        
        // 对密码进行BCrypt加密
        String encryptedPassword = passwordEncoder.encode(newPassword);
        
        log.info("开始初始化用户密码，用户数量: {}, 是否指定用户: {}", 
                CollectionUtils.isEmpty(request.getUserIds()) ? "所有用户" : request.getUserIds().size(), 
                !CollectionUtils.isEmpty(request.getUserIds()));
        
        int result = userMapper.updateUserPasswords(request.getUserIds(), encryptedPassword, LocalDateTime.now());
        
        if (result <= 0) {
            log.warn("没有用户密码被更新");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        log.info("用户密码初始化成功，影响用户数量: {}", result);
        return result;
    }
} 