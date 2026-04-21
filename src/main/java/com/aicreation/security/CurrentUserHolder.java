package com.aicreation.security;

import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;

/**
 * 当前线程的登录用户（仅 Web 请求在拦截器中设置，用后务必清理）
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUserInfo> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUserInfo info) {
        HOLDER.set(info);
    }

    public static CurrentUserInfo get() {
        return HOLDER.get();
    }

    /**
     * 获取已登录用户；未登录时统一抛业务异常，避免各处重复判空。
     */
    public static CurrentUserInfo requireAuthenticated() {
        CurrentUserInfo info = HOLDER.get();
        if (info == null || !info.isAuthenticated()) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
        }
        return info;
    }

    public static Long requireUserId() {
        return requireAuthenticated().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
