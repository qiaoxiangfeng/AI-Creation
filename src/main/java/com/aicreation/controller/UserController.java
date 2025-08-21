package com.aicreation.controller;

import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.dto.UserCreateReqDto;
import com.aicreation.entity.dto.UserLoginReqDto;
import com.aicreation.entity.dto.UserQueryReqDto;
import com.aicreation.entity.dto.UserListReqDto;
import com.aicreation.entity.dto.UserUpdateReqDto;
import com.aicreation.entity.dto.UserDeleteReqDto;
import com.aicreation.entity.dto.UserPasswordInitReqDto;
import com.aicreation.entity.dto.UserRespDto;
// import com.aicreation.entity.dto.UserListRespDto;
import com.aicreation.service.IUserService;
import com.aicreation.entity.dto.base.PageRespDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * 用户控制器
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Tag(name = "用户管理", description = "用户相关接口")
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;


    /**
     * 根据用户ID查询用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @Operation(summary = "根据用户ID查询用户", description = "根据用户ID查询用户详细信息")
    @PostMapping("/query")
    public BaseResponse<UserRespDto> getUserById(
            @Parameter(description = "用户查询请求", required = true)
            @Valid @RequestBody UserQueryReqDto request) {
        
        log.info("查询用户信息，用户ID: {}", request.getUserId());
        UserRespDto resp = userService.getUserById(request);
        return BaseResponse.success(resp);
    }

    /**
     * 创建用户
     * 
     * @param userDto 用户DTO
     * @return 创建结果
     */
    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public BaseResponse<Long> createUser(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody UserCreateReqDto req) {
        
        log.info("创建用户，用户名: {}", req.getUserName());
        Long userId = userService.createUser(req);
        return BaseResponse.success(userId);
    }

    /**
     * 更新用户信息
     * 
     * @param request 用户更新信息
     * @return 更新结果
     */
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(
            @Parameter(description = "用户更新信息", required = true)
            @Valid @RequestBody UserUpdateReqDto request) {
        
        Long userId = request.getUserId();
        log.info("更新用户信息，用户ID: {}", userId);
        Boolean result = userService.updateUser(request);
        return BaseResponse.success(true);
    }

    /**
     * 删除用户
     * 
     * @param request 删除用户请求
     * @return 删除结果
     */
    @Operation(summary = "删除用户", description = "删除指定用户")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(
            @Parameter(description = "删除用户请求", required = true)
            @Valid @RequestBody UserDeleteReqDto request) {
        
        Long userId = request.getUserId();
        log.info("删除用户，用户ID: {}", userId);
        Boolean result = userService.deleteUser(request);
        return BaseResponse.success(true);
    }

    /**
     * 查询用户列表
     * 
     * @param request 分页参数
     * @return 用户列表
     */
    @Operation(summary = "查询用户列表", description = "分页查询用户列表，支持用户名搜索")
    @PostMapping("/list")
    public PageRespDto<UserRespDto> getUserList(
            @Parameter(description = "分页参数", required = true)
            @Valid @RequestBody UserListReqDto request) {
        
        log.info("查询用户列表，页码: {}, 每页大小: {}, 用户名搜索: {}", request.getPageNum(), request.getPageSize(), request.getUserName());
        return userService.getUserList(request);
    }

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录结果
     */
    @Operation(summary = "用户登录", description = "用户登录验证")
    @PostMapping("/login")
    public BaseResponse<UserRespDto> login(
            @Parameter(description = "登录请求", required = true)
            @Valid @RequestBody UserLoginReqDto request) {
        
        log.info("用户登录，用户名: {}", request.getUserName());
        UserRespDto user = userService.login(request);
        return BaseResponse.success(user);
    }

    /**
     * 初始化用户密码
     * 
     * @param request 密码初始化请求
     * @return 初始化结果
     */
    @Operation(summary = "初始化用户密码", description = "初始化指定用户或所有用户的密码为123456")
    @PostMapping("/init-password")
    public BaseResponse<Integer> initializeUserPasswords(
            @Parameter(description = "密码初始化请求", required = true)
            @Valid @RequestBody UserPasswordInitReqDto request) {
        
        log.info("初始化用户密码，用户数量: {}, 是否指定用户: {}", 
                CollectionUtils.isEmpty(request.getUserIds()) ? "所有用户" : request.getUserIds().size(), 
                !CollectionUtils.isEmpty(request.getUserIds()));
        
        Integer result = userService.initializeUserPasswords(request);
        return BaseResponse.success(result);
    }

    // 转换逻辑由 MapStruct 的 UserConverter 负责
} 