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
     */
    @Scheduled(cron = "0 */15 * * * ?") // 每15分钟执行一次
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
            List<ArticleChapter> chaptersToProcess = articleChapterMapper.selectChaptersWithoutContent();

            if (chaptersToProcess == null || chaptersToProcess.isEmpty()) {
                log.info("没有需要生成内容的章节");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (ArticleChapter chapter : chaptersToProcess) {
                try {
                    generateContentForChapter(chapter);
                    successCount++;
                    log.info("章节[ID:{}]内容生成完成", chapter.getId());

                    // 添加短暂延迟，避免API调用过于频繁
                    Thread.sleep(3000);

                } catch (Exception e) {
                    log.error("生成章节[ID:{}]内容失败：{}", chapter.getId(), e.getMessage(), e);
                    failCount++;
                }
            }

            log.info("文章内容生成定时任务完成：成功{}个，失败{}个", successCount, failCount);
        } catch (Exception e) {
            log.error("文章内容生成定时任务执行失败：{}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 为单个章节生成内容
     */
    private void generateContentForChapter(ArticleChapter chapter) {
        try {
            // 获取文章信息
            Article article = articleMapper.selectByPrimaryKey(chapter.getArticleId());
            if (article == null) {
                log.warn("章节[ID:{}]对应的文章不存在", chapter.getId());
                return;
            }

            // 获取文章生成配置
            ArticleGenerationConfig config = findArticleGenerationConfig(article);

            // 获取本章的伏笔信息
            List<Plot> chapterPlots = plotMapper.selectByChapterId(chapter.getId());

            // 生成章节内容
            String chapterContent = generateChapterContent(article, chapter, config, chapterPlots);

            // 更新章节内容
            chapter.setChapterContent(chapterContent);
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);

        } catch (Exception e) {
            log.error("生成章节[ID:{}]内容时发生异常：{}", chapter.getId(), e.getMessage(), e);
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