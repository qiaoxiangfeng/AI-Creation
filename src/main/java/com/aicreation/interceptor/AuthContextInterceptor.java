package com.aicreation.interceptor;

import com.aicreation.entity.po.User;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.UserMapper;
import com.aicreation.security.CurrentUserHolder;
import com.aicreation.security.CurrentUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 根据请求头 X-User-Id 解析当前用户并写入 {@link CurrentUserHolder}，供列表数据权限与资源访问校验使用。
 */
@Slf4j
@Component
public class AuthContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String SESSION_USER_ID = "CURRENT_USER_ID";

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String raw = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(raw)) {
            Object sid = request.getSession(false) == null ? null : request.getSession(false).getAttribute(SESSION_USER_ID);
            if (sid != null) {
                raw = String.valueOf(sid);
            }
        }
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
        }
        try {
            long id = Long.parseLong(raw.trim());
            if (id <= 0) {
                throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
            }
            User user = userMapper.selectByPrimaryKey(id);
            if (user == null || user.getUserStatus() == null || user.getUserStatus() != 1) {
                throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
            }
            boolean isAdmin = Boolean.TRUE.equals(user.getIsAdmin());
            CurrentUserHolder.set(CurrentUserInfo.of(id, isAdmin));
        } catch (NumberFormatException e) {
            log.debug("无效的 X-User-Id: {}", raw);
            throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
