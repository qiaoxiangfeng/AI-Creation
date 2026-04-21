package com.aicreation.service.impl;

import com.aicreation.entity.po.UserNotification;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.UserNotificationMapper;
import com.aicreation.security.CurrentUserHolder;
import com.aicreation.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Override
    public List<UserNotification> listCurrentUserNotifications() {
        Long userId = CurrentUserHolder.requireUserId();
        return userNotificationMapper.selectByUserId(userId);
    }

    @Override
    public long countCurrentUserUnread() {
        Long userId = CurrentUserHolder.requireUserId();
        Long n = userNotificationMapper.countUnreadByUserId(userId);
        return n == null ? 0L : n.longValue();
    }

    @Override
    public boolean markRead(long id) {
        if (id <= 0) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        Long userId = CurrentUserHolder.requireUserId();
        UserNotification n = userNotificationMapper.selectByPrimaryKey(id);
        if (n == null) throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        if (!Objects.equals(userId, n.getUserId())) throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);

        if (Boolean.TRUE.equals(n.getIsRead())) return true;
        UserNotification upd = new UserNotification();
        upd.setId(id);
        upd.setIsRead(true);
        upd.setReadTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        return userNotificationMapper.updateByPrimaryKey(upd) > 0;
    }

    @Override
    public boolean markAllRead() {
        Long userId = CurrentUserHolder.requireUserId();
        List<UserNotification> list = userNotificationMapper.selectByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        boolean ok = true;
        for (UserNotification n : list) {
            if (Boolean.TRUE.equals(n.getIsRead())) continue;
            UserNotification upd = new UserNotification();
            upd.setId(n.getId());
            upd.setIsRead(true);
            upd.setReadTime(now);
            upd.setUpdateTime(now);
            ok = ok && userNotificationMapper.updateByPrimaryKey(upd) > 0;
        }
        return ok;
    }
}

