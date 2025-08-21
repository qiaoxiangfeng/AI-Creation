package com.aicreation.entity.bo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String userPassword;
    private String userEmail;
    private String userPhone;
    private Integer userStatus;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer resState;
}


