package com.aicreation.service;

import com.aicreation.entity.po.Article;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.mapper.ArticleMapper;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responses API response_id 管理器
 * 负责统一管理文章的response_id，确保所有AI交互都使用正确的上下文
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
@Service
public class ResponseIdManager {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    /**
     * 为指定文章调用Responses API并更新response_id
     * 使用数据库行级锁确保同一篇文章的并发AI调用安全
     *
     * @param article 文章对象
     * @param prompt AI提示词
     * @return AI生成的响应内容
     */
    @Transactional(rollbackFor = Exception.class)
    public String callAIWithResponsesAPI(Article article, String prompt) {
        return callAIWithResponsesAPI(article, prompt, "content");
    }

    @Transactional(rollbackFor = Exception.class)
    public String callAIWithResponsesAPI(Article article, String prompt, String taskType) {
        // 先通过select for update获取行级锁，确保并发安全
        Article lockedArticle = articleMapper.selectByPrimaryKeyForUpdate(article.getId());
        if (lockedArticle == null) {
            throw new RuntimeException("文章不存在: " + article.getId());
        }

        return callAIWithResponsesAPIInternal(lockedArticle, prompt, lockedArticle.getResponseId(), taskType);
    }

    /**
     * 内部方法：执行Responses API调用（已获取锁）
     *
     * @param lockedArticle 已锁定文章对象
     * @param prompt AI提示词
     * @param previousResponseId 上一轮的response_id，可为null
     * @param taskType 任务类型，用于选择合适的模型
     * @return AI生成的响应内容
     */
    private String callAIWithResponsesAPIInternal(Article lockedArticle, String prompt, String previousResponseId, String taskType) {
        try {
            // 决定是否为续写：如果已有response_id则为续写，否则为新开始
            boolean isContinue = previousResponseId != null && !previousResponseId.trim().isEmpty();

            if (isContinue) {
                log.debug("文章[{}]已有response_id，进行续写上下文关联", lockedArticle.getArticleName());
            } else {
                log.debug("文章[{}]首次生成内容，开启新的小说上下文", lockedArticle.getArticleName());
            }

            // 调用Responses API
            ResponseObject response = volcengineChatClient.createResponse(
                    prompt,
                    previousResponseId,
                    taskType
            );

            // 获取生成的內容
            String generatedContent = com.aicreation.external.ResponseContentExtractor.extractContent(response);

            // 更新文章的response_id，用于后续续写
            String newResponseId = response.getId();
            lockedArticle.setResponseId(newResponseId);
            lockedArticle.setUpdateTime(java.time.LocalDateTime.now());
            articleMapper.updateByPrimaryKey(lockedArticle);

            log.debug("文章[{}]的response_id已更新为: {}", lockedArticle.getArticleName(), newResponseId);

            return generatedContent;

        } catch (Exception e) {
            log.error("文章[{}] Responses API调用失败: {}", lockedArticle.getArticleName(), e.getMessage(), e);
            throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
        }
    }


    /**
     * 检查文章是否已有有效的response_id
     *
     * @param article 文章对象
     * @return 是否已有有效的response_id
     */
    public boolean hasValidResponseId(Article article) {
        return article.getResponseId() != null && !article.getResponseId().trim().isEmpty();
    }
}