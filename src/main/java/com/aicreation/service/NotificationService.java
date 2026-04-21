package com.aicreation.service;

import com.aicreation.entity.po.UserNotification;

import java.util.List;

public interface NotificationService {
    List<UserNotification> listCurrentUserNotifications();

    long countCurrentUserUnread();

    boolean markRead(long id);

    boolean markAllRead();
}

