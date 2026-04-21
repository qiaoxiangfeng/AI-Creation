package com.aicreation.security;

import java.io.Serializable;

/**
 * 当前请求关联的用户信息（由 {@link AuthContextInterceptor} 根据请求头解析）
 */
public class CurrentUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final boolean isAdmin;
    private final boolean authenticated;

    private CurrentUserInfo(Long userId, boolean isAdmin, boolean authenticated) {
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.authenticated = authenticated;
    }

    public static CurrentUserInfo unauthenticated() {
        return new CurrentUserInfo(null, false, false);
    }

    public static CurrentUserInfo of(Long userId, boolean isAdmin) {
        return new CurrentUserInfo(userId, isAdmin, true);
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
