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
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConverter {

	/**
	 * 提供 MapStruct 的默认实例，便于在非 Spring 上下文中直接使用
	 */
	UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    UserBo toUserBo(UserCreateReqDto request);

    @Mapping(target = "id", source = "userId")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserBo toUserBo(UserUpdateReqDto request);

    @Mapping(source = "isAdmin", target = "isAdmin")
    @Mapping(target = "membershipActive", ignore = true)
    UserRespDto toUserRespDto(User user);
}


