package com.aicreation.mapper;

import com.aicreation.entity.po.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserNotificationMapper {

    UserNotification selectByPrimaryKey(@Param("id") Long id);

    int insert(UserNotification notification);

    int updateByPrimaryKey(UserNotification notification);

    List<UserNotification> selectByUserId(@Param("userId") Long userId);

    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 根据关联业务去重通知（防止回调/对账重复触发）
     */
    Long countByBizRef(
            @Param("userId") Long userId,
            @Param("bizRefType") String bizRefType,
            @Param("bizRefId") Long bizRefId
    );
}

