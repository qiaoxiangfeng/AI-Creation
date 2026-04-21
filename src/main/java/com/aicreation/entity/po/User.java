package com.aicreation.entity.po;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public class User {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记（1-有效，0-无效）
     */
    private Integer resState;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 笔名
     */
    private String penName;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户状态（1-启用，0-禁用）
     */
    private Integer userStatus;

    /**
     * 是否管理员
     */
    private Boolean isAdmin;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 会员等级：NONE / BASIC
     */
    private String membershipTier;

    /**
     * 首次成为会员时间
     */
    private LocalDateTime membershipStartAt;

    /**
     * 会员权益截止时间
     */
    private LocalDateTime membershipEndAt;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPenName() {
        return penName;
    }

    public void setPenName(String penName) {
        this.penName = penName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Integer getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Integer userStatus) {
        this.userStatus = userStatus;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean admin) {
        this.isAdmin = admin;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(String membershipTier) {
        this.membershipTier = membershipTier;
    }

    public LocalDateTime getMembershipStartAt() {
        return membershipStartAt;
    }

    public void setMembershipStartAt(LocalDateTime membershipStartAt) {
        this.membershipStartAt = membershipStartAt;
    }

    public LocalDateTime getMembershipEndAt() {
        return membershipEndAt;
    }

    public void setMembershipEndAt(LocalDateTime membershipEndAt) {
        this.membershipEndAt = membershipEndAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getResState() {
        return resState;
    }

    public void setResState(Integer resState) {
        this.resState = resState;
    }
}

