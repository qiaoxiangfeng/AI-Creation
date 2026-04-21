package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户响应数据")
public class UserRespDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String penName;
    private String userEmail;
    private String userPhone;
    private Integer userStatus;
    private Boolean isAdmin;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Schema(description = "会员等级 NONE/BASIC")
    private String membershipTier;
    @Schema(description = "会员开始时间")
    private LocalDateTime membershipStartAt;
    @Schema(description = "会员结束时间")
    private LocalDateTime membershipEndAt;
    @Schema(description = "当前是否在会员有效期内（非管理员口径）")
    private Boolean membershipActive;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPenName() { return penName; }
    public void setPenName(String penName) { this.penName = penName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public Integer getUserStatus() { return userStatus; }
    public void setUserStatus(Integer userStatus) { this.userStatus = userStatus; }
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getMembershipTier() { return membershipTier; }
    public void setMembershipTier(String membershipTier) { this.membershipTier = membershipTier; }
    public LocalDateTime getMembershipStartAt() { return membershipStartAt; }
    public void setMembershipStartAt(LocalDateTime membershipStartAt) { this.membershipStartAt = membershipStartAt; }
    public LocalDateTime getMembershipEndAt() { return membershipEndAt; }
    public void setMembershipEndAt(LocalDateTime membershipEndAt) { this.membershipEndAt = membershipEndAt; }
    public Boolean getMembershipActive() { return membershipActive; }
    public void setMembershipActive(Boolean membershipActive) { this.membershipActive = membershipActive; }
}



