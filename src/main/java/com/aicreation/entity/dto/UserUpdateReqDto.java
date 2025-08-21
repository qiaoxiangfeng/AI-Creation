package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户更新请求")
public class UserUpdateReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "用户名", example = "updateduser")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String userName;

    @Schema(description = "用户邮箱", example = "updated@example.com")
    @Email(message = "邮箱格式不正确")
    private String userEmail;

    @Schema(description = "用户手机号", example = "13900139000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String userPhone;

    @Schema(description = "新密码", example = "newpassword123")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String userPassword;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserPassword() { return userPassword; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
}



