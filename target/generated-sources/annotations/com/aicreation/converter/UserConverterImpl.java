package com.aicreation.converter;

import com.aicreation.entity.bo.UserBo;
import com.aicreation.entity.dto.UserCreateReqDto;
import com.aicreation.entity.dto.UserRespDto;
import com.aicreation.entity.dto.UserUpdateReqDto;
import com.aicreation.entity.po.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-21T13:41:16+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250729-0351, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class UserConverterImpl implements UserConverter {

    @Override
    public UserBo toUserBo(UserCreateReqDto request) {
        if ( request == null ) {
            return null;
        }

        UserBo userBo = new UserBo();

        userBo.setUserEmail( request.getUserEmail() );
        userBo.setUserName( request.getUserName() );
        userBo.setUserPassword( request.getUserPassword() );
        userBo.setUserPhone( request.getUserPhone() );

        return userBo;
    }

    @Override
    public UserBo toUserBo(UserUpdateReqDto request) {
        if ( request == null ) {
            return null;
        }

        UserBo userBo = new UserBo();

        userBo.setId( request.getUserId() );
        userBo.setUserEmail( request.getUserEmail() );
        userBo.setUserName( request.getUserName() );
        userBo.setUserPassword( request.getUserPassword() );
        userBo.setUserPhone( request.getUserPhone() );

        return userBo;
    }

    @Override
    public UserRespDto toUserRespDto(User user) {
        if ( user == null ) {
            return null;
        }

        UserRespDto userRespDto = new UserRespDto();

        userRespDto.setId( user.getId() );
        userRespDto.setUserName( user.getUserName() );
        userRespDto.setUserEmail( user.getUserEmail() );
        userRespDto.setUserPhone( user.getUserPhone() );
        userRespDto.setUserStatus( user.getUserStatus() );
        userRespDto.setLastLoginTime( user.getLastLoginTime() );
        userRespDto.setCreateTime( user.getCreateTime() );
        userRespDto.setUpdateTime( user.getUpdateTime() );

        return userRespDto;
    }
}
