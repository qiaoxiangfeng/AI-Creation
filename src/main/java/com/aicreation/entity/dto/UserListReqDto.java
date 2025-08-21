package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.PageReqDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户列表查询请求")
public class UserListReqDto extends PageReqDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名搜索关键词（可选）", example = "admin")
    @Size(max = 50, message = "用户名搜索关键词不能超过50个字符")
    private String userName;

    // 兼容旧字段名 pageNum，与 PageReqDto 的 pageNo 映射
    @Schema(description = "页码", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码必须大于0")
    public Integer getPageNum() { return getPageNo(); }
    public void setPageNum(Integer pageNum) { setPageNo(pageNum); }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}



