package com.aicreation.service;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务状态清理服务
 * 用于清理因系统重启等原因导致的卡住任务状态
 *
 * @author AI-Creation Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TaskStatusCleanupService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleChapterMapper articleChapterMapper;

    /**
     * 清理卡住的文章生成状态
     * 将长时间处于"生成中"状态的文章重置为"未生成"
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupStuckArticleStatuses() {
        try {
            log.info("开始清理卡住的文章生成状态");

            // 查询所有处于生成中状态的文章
            List<Article> stuckArticles = articleMapper.selectArticlesByGenerationStatus(1);
            int cleanedCount = 0;

            for (Article article : stuckArticles) {
                // 检查文章是否卡住（创建时间超过1小时且仍在生成中）
                if (article.getCreateTime() != null) {
                    LocalDateTime stuckThreshold = LocalDateTime.now().minusHours(1);
                    if (article.getCreateTime().isBefore(stuckThreshold)) {
                        log.warn("发现卡住的文章生成任务，准备重置状态：articleId={}, createTime={}",
                                article.getId(), article.getCreateTime());

                        // 重置文章状态为未生成
                        article.setGenerationStatus(0);
                        article.setUpdateTime(LocalDateTime.now());
                        articleMapper.updateByPrimaryKey(article);

                        cleanedCount++;
                    }
                }
            }

            log.info("文章状态清理完成，共清理{}个卡住的任务", cleanedCount);

        } catch (Exception e) {
            log.error("清理文章状态时发生异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 清理卡住的章节生成状态
     * 将长时间处于"生成中"状态的章节重置为"未生成"
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupStuckChapterStatuses() {
        try {
            log.info("开始清理卡住的章节生成状态");

            // 查询所有处于生成中状态的章节
            List<ArticleChapter> stuckChapters = articleChapterMapper.selectChaptersByGenerationStatus(1);
            int cleanedCount = 0;

            for (ArticleChapter chapter : stuckChapters) {
                // 检查章节是否卡住（创建时间超过1小时且仍在生成中）
                if (chapter.getCreateTime() != null) {
                    LocalDateTime stuckThreshold = LocalDateTime.now().minusHours(1);
                    if (chapter.getCreateTime().isBefore(stuckThreshold)) {
                        log.warn("发现卡住的章节生成任务，准备重置状态：chapterId={}, createTime={}",
                                chapter.getId(), chapter.getCreateTime());

                        // 重置章节状态为未生成
                        chapter.setGenerationStatus(0);
                        chapter.setUpdateTime(LocalDateTime.now());
                        articleChapterMapper.updateByPrimaryKey(chapter);

                        cleanedCount++;
                    }
                }
            }

            log.info("章节状态清理完成，共清理{}个卡住的任务", cleanedCount);

        } catch (Exception e) {
            log.error("清理章节状态时发生异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 执行完整的状态清理
     */
    public void cleanupAllStuckStatuses() {
        log.info("开始执行完整的任务状态清理");
        cleanupStuckArticleStatuses();
        cleanupStuckChapterStatuses();
        log.info("完整的任务状态清理执行完毕");
    }

    /**
     * 定时任务：每小时执行一次状态清理
     * 清理超过1小时仍处于生成中的任务状态
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void scheduledCleanupStuckStatuses() {
        log.info("开始执行定时任务状态清理");
        try {
            cleanupStuckArticleStatuses();
            cleanupStuckChapterStatuses();
            log.info("定时任务状态清理执行完毕");
        } catch (Exception e) {
            log.error("定时任务状态清理失败：{}", e.getMessage(), e);
        }
    }
}