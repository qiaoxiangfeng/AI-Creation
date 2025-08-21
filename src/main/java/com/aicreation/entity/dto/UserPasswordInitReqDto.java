package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UserPasswordInitReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Size(max = 1000, message = "用户ID列表不能超过1000个")
    private List<Long> userIds;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String newPassword;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}



