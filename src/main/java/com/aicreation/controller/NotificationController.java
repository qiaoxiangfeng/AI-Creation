package com.aicreation.controller;

import com.aicreation.entity.dto.NotificationReadReqDto;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.po.UserNotification;
import com.aicreation.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息通知", description = "用户消息通知接口")
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "消息列表", description = "查询当前用户消息（按时间倒序）")
    @PostMapping("/list")
    public BaseResponse<List<UserNotification>> list() {
        return BaseResponse.success(notificationService.listCurrentUserNotifications());
    }

    @Operation(summary = "未读数量", description = "查询当前用户未读消息数量")
    @GetMapping("/unread-count")
    public BaseResponse<Long> unreadCount() {
        return BaseResponse.success(notificationService.countCurrentUserUnread());
    }

    @Operation(summary = "标记已读", description = "标记单条消息为已读")
    @PostMapping("/read")
    public BaseResponse<Boolean> read(@Valid @RequestBody NotificationReadReqDto req) {
        return BaseResponse.success(notificationService.markRead(req.getId()));
    }

    @Operation(summary = "全部已读", description = "标记全部消息为已读")
    @PostMapping("/read-all")
    public BaseResponse<Boolean> readAll() {
        return BaseResponse.success(notificationService.markAllRead());
    }
}

