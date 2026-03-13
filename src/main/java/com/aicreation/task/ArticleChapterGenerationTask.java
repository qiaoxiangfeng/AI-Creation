package com.aicreation.task;

import com.aicreation.entity.po.Article;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.generate.ArticleChapterGenerator;
import com.aicreation.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章章节生成定时任务
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Component
public class ArticleChapterGenerationTask {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleChapterMapper articleChapterMapper;

    @Autowired
    private ArticleChapterGenerator articleChapterGenerator;

    /**
     * 任务执行状态标记，避免并发执行
     */
    private final java.util.concurrent.atomic.AtomicBoolean isRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 定时任务：每月1号执行文章章节生成
     * 如果上次执行未完成，则丢弃本次调度
     * 数据实时落表，无事务保护，确保服务重启时数据不丢失
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 每月1号0点0分0秒执行
    public void generateArticleChapters() {
        TraceUtil.executeWithTraceId(() -> {
            executeArticleChaptersTask();
        });
    }

    /**
     * 执行文章章节生成任务的具体逻辑
     */
    private void executeArticleChaptersTask() {
        // 检查任务是否正在执行，如果是则跳过本次执行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("文章章节生成定时任务正在执行中，跳过本次调度");
            return;
        }

        log.info("开始执行文章章节生成定时任务");

        try {
            // 查询需要生成章节的文章（没有章节的文章）
            List<Article> articlesToProcess = articleMapper.selectArticlesWithoutChapters();

            if (articlesToProcess == null || articlesToProcess.isEmpty()) {
                log.info("没有需要生成章节的文章");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Article article : articlesToProcess) {
                try {
                    articleChapterGenerator.generateChaptersForArticle(article.getId());
                    successCount++;
                    log.info("文章[{}]章节基本信息生成完成", article.getArticleName());

                    // 添加短暂延迟，避免API调用过于频繁
                    Thread.sleep(2000);

                } catch (Exception e) {
                    log.error("生成文章[{}]章节失败：{}", article.getArticleName(), e.getMessage(), e);
                    failCount++;
                }
            }

            log.info("文章章节生成定时任务完成：成功{}个，失败{}个", successCount, failCount);

        } catch (Exception e) {
            log.error("文章章节生成定时任务执行失败：{}", e.getMessage());
        } finally {
            isRunning.set(false);
        }
    }





    /**
     * 章节内容内部类
     */
    private static class ChapterContent {
        private String title;
        private String content;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

}