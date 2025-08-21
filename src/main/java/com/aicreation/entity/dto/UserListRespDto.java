package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "用户列表响应")
public class UserListRespDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    private List<UserRespDto> users;
    private Integer total;

    public List<UserRespDto> getUsers() { return users; }
    public void setUsers(List<UserRespDto> users) { this.users = users; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}



