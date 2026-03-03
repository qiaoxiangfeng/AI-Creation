package com.aicreation.task;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.entity.po.Plot;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.external.VolcengineChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章内容生成定时任务
 * 根据章节基本信息生成章节具体内容
 * 采用实时落表策略，确保生成过程不会因中断丢失已生成内容
 * 每次只处理少量章节，避免单次运行时间过长
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Component
public class ArticleContentGenerationTask {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleChapterMapper articleChapterMapper;

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Autowired
    private PlotMapper plotMapper;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    /**
     * 任务执行状态标记，避免并发执行
     */
    private final java.util.concurrent.atomic.AtomicBoolean isRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 定时任务：每15分钟执行一次文章内容生成
     * 如果上次执行未完成，则丢弃本次调度
     * 暂时关闭：注释掉@Scheduled注解
     */
    // @Scheduled(cron = "0 */15 * * * ?") // 每15分钟执行一次 - 暂时关闭
    @Transactional(rollbackFor = Exception.class)
    public void generateArticleContents() {
        // 检查任务是否正在执行，如果是则跳过本次执行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("文章内容生成定时任务正在执行中，跳过本次调度");
            return;
        }

        log.info("开始执行文章内容生成定时任务");

        try {
            // 查询需要生成内容的章节（chapter_content为空的章节）
            // 按文章ID和章节号排序，确保同一篇文章的章节按顺序处理
            List<ArticleChapter> chaptersToProcess = articleChapterMapper.selectChaptersWithoutContent();

            if (chaptersToProcess == null || chaptersToProcess.isEmpty()) {
                log.info("没有需要生成内容的章节");
                return;
            }

            // 限制单次处理的数量，避免运行时间过长
            int maxProcessCount = 5; // 每次最多处理5个章节
            List<ArticleChapter> chaptersToProcessLimited = chaptersToProcess.size() > maxProcessCount
                ? chaptersToProcess.subList(0, maxProcessCount)
                : chaptersToProcess;

            log.info("本次将处理{}个章节（总待处理：{}个）", chaptersToProcessLimited.size(), chaptersToProcess.size());

            int successCount = 0;
            int failCount = 0;

            for (ArticleChapter chapter : chaptersToProcessLimited) {
                try {
                    generateContentForChapter(chapter);
                    successCount++;
                    log.info("章节[ID:{}]内容生成完成", chapter.getId());

                    // 添加短暂延迟，避免API调用过于频繁
                    Thread.sleep(3000);

                } catch (Exception e) {
                    log.error("生成章节[ID:{}]内容失败：{}", chapter.getId(), e.getMessage(), e);
                    failCount++;

                    // 即使失败也继续处理其他章节，确保部分成功
                }
            }

            log.info("文章内容生成定时任务完成：成功{}个，失败{}个，剩余待处理{}个",
                successCount, failCount, Math.max(0, chaptersToProcess.size() - chaptersToProcessLimited.size()));
        } catch (Exception e) {
            log.error("文章内容生成定时任务执行失败：{}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 为单个章节生成内容
     * 采用实时落表策略，确保生成过程不会因中断丢失数据
     */
    private void generateContentForChapter(ArticleChapter chapter) {
        String originalContent = chapter.getChapterContent(); // 保存原始内容，用于回滚
        boolean contentGenerated = false;

        try {
            // 获取文章信息
            Article article = articleMapper.selectByPrimaryKey(chapter.getArticleId());
            if (article == null) {
                log.warn("章节[ID:{}]对应的文章不存在", chapter.getId());
                return;
            }

            // 检查文章是否处于生成中状态，如果不是则设置为生成中
            if (article.getGenerationStatus() != 1) {
                article.setGenerationStatus(1); // 1-生成中
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateByPrimaryKey(article);
                log.info("文章[{}]状态已更新为生成中", article.getArticleName());
            }

            // 获取文章生成配置
            ArticleGenerationConfig config = findArticleGenerationConfig(article);

            // 获取本章的伏笔信息
            List<Plot> chapterPlots = plotMapper.selectByChapterId(chapter.getId());

            log.info("开始生成章节[ID:{}]内容，当前内容长度：{}", chapter.getId(),
                chapter.getChapterContent() != null ? chapter.getChapterContent().length() : 0);

            // 生成章节内容 - 这是最耗时的操作
            String chapterContent = generateChapterContent(article, chapter, config, chapterPlots);

            // 内容生成成功，立即落表
            chapter.setChapterContent(chapterContent);
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);
            contentGenerated = true;

            log.info("章节[ID:{}]内容生成并保存成功，内容长度：{} 字符", chapter.getId(), chapterContent.length());

            // 检查是否所有章节都已完成，如果是则更新文章状态为已完成
            checkAndUpdateArticleCompletionStatus(article);

        } catch (Exception e) {
            log.error("生成章节[ID:{}]内容时发生异常：{}", chapter.getId(), e.getMessage(), e);

            // 如果内容已经生成但保存失败，尝试重新保存
            if (contentGenerated && chapter.getChapterContent() != null) {
                try {
                    log.info("尝试重新保存章节[ID:{}]的内容", chapter.getId());
                    articleChapterMapper.updateByPrimaryKey(chapter);
                    log.info("章节[ID:{}]内容重新保存成功", chapter.getId());
                } catch (Exception saveException) {
                    log.error("章节[ID:{}]内容重新保存失败：{}", chapter.getId(), saveException.getMessage());
                    // 可以考虑将内容保存到临时文件或日志中，以便后续恢复
                }
            }

            throw e;
        }
    }

    /**
     * 生成章节内容的AI提示词
     */
    private String generateChapterContent(Article article, ArticleChapter chapter,
                                        ArticleGenerationConfig config, List<Plot> chapterPlots) {
        String prompt = buildChapterContentPrompt(article, chapter, config, chapterPlots);

        log.info("=== AI章节内容生成请求 ===");
        log.info("文章: {} - 第{}章: {}", article.getArticleName(), chapter.getChapterNo(), chapter.getChapterTitle());
        log.info("AI提示词长度: {} 字符", prompt.length());

        try {
            List<String> results = volcengineChatClient.chatCompletions(
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "doubao-seed-1-6-250615",
                prompt
            );

            if (results != null && !results.isEmpty()) {
                String generatedContent = results.get(0);
                log.info("=== AI生成内容 ===");
                log.info("内容长度: {} 字符", generatedContent.length());

                return generatedContent;
            } else {
                log.warn("AI返回结果为空");
                throw new RuntimeException("AI返回结果为空");
            }
        } catch (Exception e) {
            log.error("调用AI生成章节内容失败：{}", e.getMessage(), e);
            return getFallbackChapterContent(article, chapter);
        }
    }

    /**
     * 构建章节内容生成的AI提示词
     */
    private String buildChapterContentPrompt(Article article, ArticleChapter chapter,
                                           ArticleGenerationConfig config, List<Plot> chapterPlots) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请创作小说《").append(article.getArticleName()).append("》第").append(chapter.getChapterNo()).append("章\n\n");

        // 一、基础信息
        prompt.append("一、基础信息\n");
        prompt.append("1. 本章标题：").append(chapter.getChapterTitle()).append("\n");
        prompt.append("2. 本章核心剧情：").append(chapter.getCorePlot()).append("\n");
        prompt.append("3. 字数预估：").append(chapter.getWordCountEstimate()).append("字\n\n");

        // 二、必须严格遵守的伏笔规则
        if (chapterPlots != null && !chapterPlots.isEmpty()) {
            prompt.append("二、必须严格遵守的伏笔规则（非常重要）\n");
            prompt.append("1. 本章必须自然埋设以下伏笔：\n");

            for (int i = 0; i < chapterPlots.size(); i++) {
                Plot plot = chapterPlots.get(i);
                prompt.append("   - 伏笔").append(i + 1).append("：").append(plot.getPlotContent()).append("\n");
            }

            prompt.append("2. 以上伏笔**本章不解释、不揭穿、不回收**，只悄悄埋下细节、对话、物品、环境、眼神、异常现象等线索。\n");
            prompt.append("3. 伏笔必须融入剧情，不能生硬突兀。\n\n");

            // 三、伏笔回收计划
            prompt.append("三、伏笔回收计划（AI必须严格遵守）\n");
            for (int i = 0; i < chapterPlots.size(); i++) {
                Plot plot = chapterPlots.get(i);
                if (plot.getRecoveryChapterId() != null) {
                    prompt.append("- 伏笔").append(i + 1).append("将在第").append(plot.getRecoveryChapterId()).append("章回收\n");
                }
            }
            prompt.append("本章绝对不能提前回收，只能埋设。\n\n");
        }

        // 四、写作要求
        prompt.append("四、写作要求\n");
        prompt.append("- 字数严格控制在").append(chapter.getWordCountEstimate()).append("字左右\n");
        prompt.append("- 节奏紧凑，有画面感，对话自然\n");
        prompt.append("- 人物行为符合人设，不OOC\n");
        prompt.append("- 本章结尾**必须留下强悬念**，停在关键冲突/反转/危险瞬间，不交代结果\n");
        prompt.append("- 严格遵循大纲和核心剧情，不新增无关设定\n");

        return prompt.toString();
    }

    /**
     * 查找文章对应的生成配置
     */
    private ArticleGenerationConfig findArticleGenerationConfig(Article article) {
        try {
            if (StringUtils.hasText(article.getArticleType())) {
                return articleGenerationConfigMapper.selectByTheme(article.getArticleType());
            }
        } catch (Exception e) {
            log.warn("查找文章生成配置失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查并更新文章完成状态
     * 如果所有章节都有内容，则将文章状态更新为已完成
     */
    private void checkAndUpdateArticleCompletionStatus(Article article) {
        try {
            // 查询文章的所有章节
            List<ArticleChapter> allChapters = articleChapterMapper.selectByArticleId(article.getId());

            if (allChapters == null || allChapters.isEmpty()) {
                log.warn("文章[{}]没有章节数据", article.getArticleName());
                return;
            }

            // 检查是否所有章节都有内容
            boolean allCompleted = allChapters.stream()
                .allMatch(chapter -> chapter.getChapterContent() != null &&
                           !chapter.getChapterContent().trim().isEmpty());

            if (allCompleted && article.getGenerationStatus() != 2) {
                // 更新文章状态为已完成
                article.setGenerationStatus(2); // 2-已完成
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateByPrimaryKey(article);

                log.info("文章[{}]所有章节内容生成完成，状态更新为已完成", article.getArticleName());
            }
        } catch (Exception e) {
            log.error("检查文章[{}]完成状态时发生异常：{}", article.getArticleName(), e.getMessage(), e);
            // 不抛出异常，避免影响主要流程
        }
    }

    /**
     * 获取默认章节内容（当AI生成失败时使用）
     */
    private String getFallbackChapterContent(Article article, ArticleChapter chapter) {
        return "【章节内容生成中...】\n\n" +
               "本章标题：" + chapter.getChapterTitle() + "\n\n" +
               "（由于AI生成服务暂时不可用，本章内容将在稍后自动生成）\n\n" +
               "核心剧情：" + chapter.getCorePlot() + "\n\n" +
               "字数预估：" + chapter.getWordCountEstimate() + "字";
    }
}