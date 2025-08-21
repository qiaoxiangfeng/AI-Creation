package com.aicreation.converter;

import com.aicreation.entity.bo.UserBo;
import com.aicreation.entity.dto.UserCreateReqDto;
import com.aicreation.entity.dto.UserRespDto;
import com.aicreation.entity.dto.UserUpdateReqDto;
import com.aicreation.entity.po.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserBo toUserBo(UserCreateReqDto request);

    @Mapping(target = "id", source = "userId")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserBo toUserBo(UserUpdateReqDto request);

    UserRespDto toUserRespDto(User user);
}


