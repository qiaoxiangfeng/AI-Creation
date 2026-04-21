package com.aicreation.security;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.entity.po.Dictionary;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;

/**
 * Web 请求下的文章 / 生成配置数据权限（非 Web 调用不校验，供定时任务等使用）
 */
@Service
public class AccessControlService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    private boolean isWebRequest() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    /**
     * 当前是否为 Web 请求。
     */
    public boolean isWebRequestContext() {
        return isWebRequest();
    }

    /**
     * 用于列表数据权限：管理员返回 null（不过滤），普通用户返回本人 userId。
     */
    public Long getScopedCreateUserIdForList() {
        if (!isWebRequest()) {
            return null;
        }
        CurrentUserInfo u = CurrentUserHolder.requireAuthenticated();
        return u.isAdmin() ? null : u.getUserId();
    }

    /**
     * 当前请求须为已登录的管理员（用于用户管理等后台能力）
     */
    public void assertAdmin() {
        if (!isWebRequest()) {
            return;
        }
        CurrentUserInfo u = CurrentUserHolder.requireAuthenticated();
        if (!u.isAdmin()) {
            throw new BusinessException(ErrorCodeEnum.ADMIN_ONLY);
        }
    }

    public void assertArticleAccess(Long articleId) {
        if (!isWebRequest() || articleId == null) {
            return;
        }
        CurrentUserInfo u = CurrentUserHolder.requireAuthenticated();
        if (u.isAdmin()) {
            return;
        }
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        if (!Objects.equals(u.getUserId(), article.getCreateUserId())) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_OWNED);
        }
    }

    public void assertGenerationConfigAccess(Long configId) {
        if (!isWebRequest() || configId == null) {
            return;
        }
        CurrentUserInfo u = CurrentUserHolder.requireAuthenticated();
        if (u.isAdmin()) {
            return;
        }
        ArticleGenerationConfig config = articleGenerationConfigMapper.selectByPrimaryKey(configId);
        if (config == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        if (!Objects.equals(u.getUserId(), config.getCreateUserId())) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_OWNED);
        }
    }

    /**
     * 字典可读：全局（create_user_id 为空）对所有人可见；否则仅创建人可见（需已登录）。
     */
    public void assertDictionaryReadable(Dictionary dictionary) {
        if (!isWebRequest() || dictionary == null) {
            return;
        }
        CurrentUserInfo u = CurrentUserHolder.get();
        if (u != null && u.isAuthenticated()) {
            if (dictionary.getCreateUserId() == null || Objects.equals(u.getUserId(), dictionary.getCreateUserId())) {
                return;
            }
            throw new BusinessException(ErrorCodeEnum.DICTIONARY_READ_DENIED);
        }
        if (dictionary.getCreateUserId() == null) {
            return;
        }
        throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
    }

    /**
     * 字典可写：管理员任意；全局字典仅管理员可改删；个人字典仅创建人可改删。
     */
    public void assertDictionaryWritable(Dictionary dictionary) {
        if (!isWebRequest() || dictionary == null) {
            return;
        }
        CurrentUserInfo u = CurrentUserHolder.requireAuthenticated();
        if (u.isAdmin()) {
            return;
        }
        if (dictionary.getCreateUserId() == null) {
            throw new BusinessException(ErrorCodeEnum.GLOBAL_DICTIONARY_WRITE_DENIED);
        }
        if (!Objects.equals(u.getUserId(), dictionary.getCreateUserId())) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_OWNED);
        }
    }
}
