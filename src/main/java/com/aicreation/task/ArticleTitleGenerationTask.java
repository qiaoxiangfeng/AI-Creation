package com.aicreation.task;

import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.dto.ArticleGenerationConfigListReqDto;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.enums.ArticleStatusEnum;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.generate.ArticleTitleGenerator;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.service.IArticleService;
import com.aicreation.util.TraceUtil;
import com.aicreation.service.IArticleService;
import com.aicreation.service.ResponseIdManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文章标题生成定时任务
 * 根据文章主题和待生成数量自动生成文章标题和大纲
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Component
public class ArticleTitleGenerationTask {

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Autowired
    private com.aicreation.mapper.ArticleMapper articleMapper;

    @Autowired
    private IArticleService articleService;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    @Autowired
    private ResponseIdManager responseIdManager;

    @Autowired
    private ArticleTitleGenerator articleTitleGenerator;

    /**
     * 任务执行状态标记，避免并发执行
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 定时任务：每月1号执行文章标题生成
     * 如果上次执行未完成，则丢弃本次调度
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 每月1号0点0分0秒执行
    @Transactional(rollbackFor = Exception.class)
    public void generateArticleTitles() {
        TraceUtil.executeWithTraceId(() -> {
            executeArticleTitlesTask();
        });
    }

    /**
     * 执行文章标题生成任务的具体逻辑
     */
    private void executeArticleTitlesTask() {
        // 检查任务是否正在执行，如果是则跳过本次执行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("文章标题生成定时任务正在执行中，跳过本次调度");
            return;
        }

        log.info("开始执行文章标题生成定时任务");

        try {
            // 查询所有待生成数量大于0的文章分类
            List<ArticleGenerationConfig> articleTypes = articleGenerationConfigMapper.selectPendingArticleGenerationConfigs();

            for (ArticleGenerationConfig articleType : articleTypes) {
                if (articleType.getPendingCount() != null && articleType.getPendingCount() > 0) {
                    generateTitlesForType(articleType);
                }
            }

            log.info("文章标题生成定时任务执行完成");
        } catch (Exception e) {
            log.error("文章标题生成定时任务执行失败", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        } finally {
            // 重置执行状态
            isRunning.set(false);
        }
    }

    /**
     * 为指定文章类型生成文章标题和大纲
     *
     * @param articleType 文章类型
     */
    private void generateTitlesForType(ArticleGenerationConfig articleType) {
        log.info("开始为文章主题[{}]生成{}个标题", articleType.getTheme(), articleType.getPendingCount());

        int successCount = 0;
        int failCount = 0;
        boolean hasTransactionError = false;

        for (int i = 0; i < articleType.getPendingCount(); i++) {
            try {
                // 如果之前有事务错误，停止处理
                if (hasTransactionError) {
                    log.warn("由于之前的数据库事务错误，停止处理剩余的文章生成任务");
                    break;
                }

                // 调用统一的标题生成器生成单个标题
                Long articleId = articleTitleGenerator.generateSingleTitle(articleType.getId());

                log.info("成功生成文章标题：主题={}, ID={}",
                        articleType.getTheme(), articleId);

                successCount++;

                // 添加短暂延迟，避免API调用过于频繁
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("生成文章标题失败：主题={}, 错误={}", articleType.getTheme(), e.getMessage(), e);

                // 检查是否是数据库事务相关错误
                if (e.getMessage() != null &&
                    (e.getMessage().contains("current transaction is aborted") ||
                     e.getMessage().contains("duplicate key value"))) {
                    hasTransactionError = true;
                    log.error("检测到数据库事务错误，将停止当前批次的处理");
                }

                failCount++;
            }
        }

        // 只有在没有事务错误的情况下才更新待生成数量
        if (!hasTransactionError) {
            try {
                int remainingCount = Math.max(0, articleType.getPendingCount() - successCount);
                updatePendingCount(articleType.getId(), remainingCount);
                log.info("文章主题[{}]标题生成完成：成功{}个，失败{}个，剩余待生成{}个",
                        articleType.getTheme(), successCount, failCount, remainingCount);
            } catch (Exception e) {
                log.error("更新待生成数量失败：articleTypeId={}, newCount={}, 错误={}",
                        articleType.getId(), articleType.getPendingCount() - successCount, e.getMessage(), e);
                // 不抛出异常，避免影响其他任务
            }
        } else {
            log.warn("由于数据库事务错误，跳过待生成数量的更新，建议手动检查数据一致性");
        }
    }

    /**
     * 查询指定文章类型下已存在的文章标题
     *
     * @param articleType 文章类型
     * @return 已存在的文章标题列表
     */
    private List<String> getExistingTitlesByArticleType(String articleType) {
        try {
            return articleMapper.selectExistingTitlesByArticleType(articleType);
        } catch (Exception e) {
            log.warn("查询已存在文章标题失败：articleType={}, error={}", articleType, e.getMessage());
            return new ArrayList<>();
        }
    }







    /**
     * 更新待生成数量
     *
     * @param articleTypeId 文章类型ID
     * @param newCount 新的待生成数量
     */
    private void updatePendingCount(Long articleTypeId, int newCount) {
        try {
            ArticleGenerationConfig updateType = new ArticleGenerationConfig();
            updateType.setId(articleTypeId);
            updateType.setPendingCount(newCount);
            updateType.setUpdateTime(LocalDateTime.now());

            articleGenerationConfigMapper.updateByPrimaryKey(updateType);
            log.info("更新文章分类ID={}的待生成数量为{}", articleTypeId, newCount);
        } catch (Exception e) {
            log.error("更新待生成数量失败：articleTypeId={}, newCount={}", articleTypeId, newCount, e);
        }
    }



}