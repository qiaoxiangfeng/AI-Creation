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
import com.aicreation.service.ResponseIdManager;
import com.aicreation.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private ResponseIdManager responseIdManager;

    @Autowired
    private com.aicreation.generate.ArticleContentGenerator articleContentGenerator;

    /**
     * 任务执行状态标记，避免并发执行
     */
    private final java.util.concurrent.atomic.AtomicBoolean isRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 手动触发单个章节的内容生成
     * 用于在点击生成按钮时立即生成指定章节的内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateChapterContentManually(ArticleChapter chapter) {
        articleContentGenerator.generateChapterContent(chapter.getId());
    }

    /**
     * 检查并更新文章完成状态
     * 用于在手动生成内容后检查是否所有章节都已完成
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpdateArticleCompletionStatusManually(Article article) {
        checkAndUpdateArticleCompletionStatus(article);
    }

    /**
     * 为单篇文章生成所有章节内容
     * @param article 文章对象
     * @param chapters 文章的章节列表
     */
    public void generateContentsForArticle(Article article, List<ArticleChapter> chapters) {
        log.info("开始为文章[{}]生成章节内容，共{}个章节", article.getArticleName(), chapters.size());

        int successCount = 0;
        int failCount = 0;

        for (ArticleChapter chapter : chapters) {
            try {
                // 只为没有内容且生成状态不为失败的章节生成内容
                if (StringUtils.hasText(chapter.getChapterContent()) || chapter.getGenerationStatus() == 3) {
                    log.info("章节[ID:{}]已有内容或生成失败，跳过", chapter.getId());
                    continue;
                }

                generateContentForChapter(chapter);
                successCount++;
                log.info("文章[{}]章节[ID:{}]内容生成完成", article.getArticleName(), chapter.getId());

                // 添加短暂延迟，避免API调用过于频繁
                Thread.sleep(3000);

            } catch (Exception e) {
                log.error("生成文章[{}]章节[ID:{}]内容失败：{}", article.getArticleName(), chapter.getId(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("文章[{}]章节内容生成完成，成功：{}，失败：{}", article.getArticleName(), successCount, failCount);
    }

    /**
     * 定时任务：每月1号执行文章内容生成
     * 如果上次执行未完成，则丢弃本次调度
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 每月1号0点0分0秒执行
    @Transactional(rollbackFor = Exception.class)
    public void generateArticleContents() {
        TraceUtil.executeWithTraceId(() -> {
            executeArticleContentsTask();
        });
    }

    /**
     * 执行文章内容生成任务的具体逻辑
     */
    private void executeArticleContentsTask() {
        // 检查任务是否正在执行，如果是则跳过本次执行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("文章内容生成定时任务正在执行中，跳过本次调度");
            return;
        }

        log.info("开始执行文章内容生成定时任务");

        try {
            // 查询需要生成内容的章节（chapter_content为空的章节，且文章状态不为生成中）
            // 按文章ID和章节号排序，确保同一篇文章的章节按顺序处理
            List<ArticleChapter> chaptersToProcess = articleChapterMapper.selectChaptersWithoutContentExcludingGenerating();

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
            Integer currentStatus = article.getGenerationStatus() != null ? article.getGenerationStatus() : 0;
            if (currentStatus != 1) {
                article.setGenerationStatus(1); // 1-生成中
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateByPrimaryKey(article);
                log.info("文章[{}]状态已更新为生成中", article.getArticleName());
            }

            // 获取文章生成配置
            ArticleGenerationConfig config = findArticleGenerationConfig(article);

            // 获取本章需要埋设的伏笔信息
            List<Plot> chapterPlots = plotMapper.selectByChapterId(chapter.getId());

            // 获取本章需要回收的伏笔信息
            List<Plot> recoveryPlots = plotMapper.selectByRecoveryChapterId(chapter.getId());

            log.info("开始生成章节[ID:{}]内容，当前内容长度：{}，需要埋设伏笔：{}个，需要回收伏笔：{}个",
                chapter.getId(),
                chapter.getChapterContent() != null ? chapter.getChapterContent().length() : 0,
                chapterPlots != null ? chapterPlots.size() : 0,
                recoveryPlots != null ? recoveryPlots.size() : 0);

            // 生成章节内容 - 这是最耗时的操作
            String chapterContent = generateChapterContent(article, chapter, config, chapterPlots, recoveryPlots);

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
                                        ArticleGenerationConfig config, List<Plot> chapterPlots, List<Plot> recoveryPlots) {
        String prompt = buildChapterContentPrompt(article, chapter, config, chapterPlots, recoveryPlots);

        log.info("=== AI章节内容生成请求 ===");
        log.info("文章: {} - 第{}章: {}", article.getArticleName(), chapter.getChapterNo(), chapter.getChapterTitle());
        log.info("AI提示词长度: {} 字符", prompt.length());
        log.debug("=== 完整AI提示词 ===");
        log.debug("{}", prompt);

        try {
            // 使用Responses API生成内容，支持上下文管理和多任务隔离
            log.info("开始使用Responses API生成章节内容...");
            String generatedContent = callAIWithResponsesAPI(
                article,
                prompt
            );

            log.info("=== AI生成内容 ===");
            log.info("内容长度: {} 字符", generatedContent.length());

            // 检查并处理故事完结标记
            boolean storyComplete = false;
            String cleanContent = generatedContent;
            if (generatedContent.contains("[STORY_COMPLETE]")) {
                storyComplete = true;
                cleanContent = generatedContent.replace("[STORY_COMPLETE]", "").trim();
                log.info("检测到故事完结标记，此章节后故事已完结");
            }

            // 更新章节的完结状态
            chapter.setStoryComplete(storyComplete);

            return cleanContent;
        } catch (Exception e) {
            log.error("调用AI生成章节内容失败：{}", e.getMessage(), e);
            throw new RuntimeException("AI生成章节内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用Responses API调用AI，支持上下文管理和多任务隔离
     */
    private String callAIWithResponsesAPI(Article article, String prompt) {
        return responseIdManager.callAIWithResponsesAPI(article, prompt);
    }

    /**
     * 构建章节内容生成的AI提示词
     */
    private String buildChapterContentPrompt(Article article, ArticleChapter chapter,
                                           ArticleGenerationConfig config, List<Plot> chapterPlots, List<Plot> recoveryPlots) {
        StringBuilder prompt = new StringBuilder();
        int sectionCounter = 1;

        prompt.append("请创作小说《").append(article.getArticleName()).append("》第").append(chapter.getChapterNo()).append("章\n\n");
        prompt.append("【重要要求】：只输出纯小说正文内容，不要添加任何提示、注释、元信息、格式标记、章节标题或说明文字。直接从故事叙述开始，纯文本输出。\n\n");

        // 一、故事整体信息
        prompt.append(sectionCounter++).append("、故事整体信息\n");
        if (StringUtils.hasText(article.getArticleOutline())) {
            prompt.append("1. 故事大纲：").append(article.getArticleOutline()).append("\n");
        }
        if (StringUtils.hasText(article.getStoryBackground())) {
            prompt.append("2. 故事背景：").append(article.getStoryBackground()).append("\n");
        }
        prompt.append("\n");

        // 二、前文参考（确保故事连贯性）
        List<ArticleChapter> previousChapters = getPreviousChapters(article.getId(), chapter.getChapterNo());
        if (previousChapters != null && !previousChapters.isEmpty()) {
            prompt.append(sectionCounter++).append("、前文参考（确保故事连贯性）\n");
            prompt.append("以下是前几章的关键内容摘要，仅供参考当前章节的写作脉络，请勿照抄：\n\n");

            for (ArticleChapter prevChapter : previousChapters) {
                if (StringUtils.hasText(prevChapter.getChapterContent())) {
                    String summary = summarizeChapterContent(prevChapter.getChapterContent());
                    prompt.append("第").append(prevChapter.getChapterNo()).append("章 ").append(prevChapter.getChapterTitle()).append("：\n");
                    prompt.append(summary).append("\n\n");
                }
            }
            prompt.append("请基于以上前文自然衔接，保持人物关系、情节发展的一致性。\n\n");
        }

        // 三、写作风格指导
        if (config != null) {
            prompt.append(sectionCounter++).append("、写作风格指导\n");
            if (StringUtils.hasText(config.getGender())) {
                prompt.append("1. 受众性别：").append(config.getGender()).append("\n");
            }
            if (StringUtils.hasText(config.getGenre())) {
                prompt.append("2. 题材类型：").append(config.getGenre()).append("，请遵循该题材的叙事风格和世界观设定\n");
            }
            if (StringUtils.hasText(config.getStyle())) {
                prompt.append("3. 写作风格：").append(config.getStyle()).append("\n");
            }
            if (StringUtils.hasText(config.getPlot())) {
                prompt.append("4. 情节类型：").append(config.getPlot()).append("，请体现该情节类型的核心特征\n");
            }
            prompt.append("\n");
        }

        // 四、本章基础信息
        prompt.append(sectionCounter++).append("、本章基础信息\n");
        prompt.append("1. 本章标题：").append(chapter.getChapterTitle()).append("\n");
        prompt.append("2. 本章核心剧情：").append(chapter.getCorePlot()).append("\n");
        prompt.append("3. 字数预估：").append(chapter.getWordCountEstimate()).append("字\n\n");

        // 五、必须严格遵守的伏笔规则
        if (chapterPlots != null && !chapterPlots.isEmpty()) {
            prompt.append(sectionCounter++).append("、必须严格遵守的伏笔规则（非常重要）\n");
            prompt.append("1. 本章必须自然埋设以下伏笔：\n");

            for (int i = 0; i < chapterPlots.size(); i++) {
                Plot plot = chapterPlots.get(i);
                prompt.append("   - 伏笔").append(i + 1).append("：").append(plot.getPlotContent()).append("\n");
            }

            prompt.append("2. 伏笔埋设技巧：\n");
            prompt.append("   - 融入环境描写：通过异常的物品、特殊的场景、反常的现象等\n");
            prompt.append("   - 融入人物对话：通过只言片语、欲言又止的表情、闪烁其词的回答\n");
            prompt.append("   - 融入人物行为：通过不经意的动作、短暂的犹豫、异常的反应\n");
            prompt.append("   - 融入心理描写：通过主角的直觉、预感、回忆片段\n");
            prompt.append("3. 以上伏笔**本章绝对不能解释、揭穿或回收**，只能自然埋设线索。\n");
            prompt.append("4. 伏笔必须服务于故事发展，不能喧宾夺主。\n\n");

            // 六、伏笔回收计划
            prompt.append(sectionCounter++).append("、伏笔回收计划（AI必须严格遵守）\n");
            for (int i = 0; i < chapterPlots.size(); i++) {
                Plot plot = chapterPlots.get(i);
                if (plot.getRecoveryChapterId() != null) {
                    prompt.append("- 伏笔").append(i + 1).append("将在第").append(plot.getRecoveryChapterId()).append("章回收\n");
                }
            }
            prompt.append("本章绝对不能提前回收，只能埋设。\n\n");
        }

        // 七、伏笔回收指导
        if (recoveryPlots != null && !recoveryPlots.isEmpty()) {
            prompt.append(sectionCounter++).append("、伏笔回收指导（非常重要）\n");
            prompt.append("本章必须回收以下伏笔，回收时要自然融入剧情发展中：\n");

            for (int i = 0; i < recoveryPlots.size(); i++) {
                Plot plot = recoveryPlots.get(i);
                prompt.append("1. 回收伏笔").append(i + 1).append("：").append(plot.getPlotContent()).append("\n");
                prompt.append("   - 伏笔来源：").append(plot.getPlotName()).append("\n");
                prompt.append("   - 回收要求：通过剧情发展自然揭示，制造惊喜或转折效果\n");
            }

            prompt.append("\n伏笔回收技巧：\n");
            prompt.append("- 渐进揭示：通过对话、行为、环境变化逐步揭开真相\n");
            prompt.append("- 制造转折：回收伏笔时制造剧情转折，增强戏剧性\n");
            prompt.append("- 情感冲击：回收伏笔要产生情感冲击，让读者产生恍然大悟的感觉\n");
            prompt.append("- 自然过渡：回收过程要融入正常剧情，不要生硬中断\n\n");
        }

        // 八、写作要求
        prompt.append(sectionCounter++).append("、写作要求\n");
        prompt.append("1. 字数控制：严格控制在").append(chapter.getWordCountEstimate()).append("字左右\n");
        prompt.append("2. 叙事风格：节奏紧凑，描写生动有画面感，对话符合人物性格和社会背景\n");
        prompt.append("3. 人物塑造：人物行为符合人设，不出现OOC（角色崩坏）情况\n");
        prompt.append("4. 章节结构：\n");
        prompt.append("   - 开头：自然衔接上一章，快速进入本章核心剧情\n");
        prompt.append("   - 中间：层层推进，逐步展开冲突，适时埋设伏笔\n");
        prompt.append("   - 结尾：**必须留下强悬念**，停在最扣人心弦的瞬间\n");
        prompt.append("5. 悬念设计：停在关键冲突/意外反转/突发危机/重要抉择前一刻，制造急迫感让读者想看下一章\n");
        prompt.append("6. 内容约束：严格遵循大纲和核心剧情，不新增无关设定，不偏离故事主线\n");
        prompt.append("7. 语言要求：用词精准，避免重复，符合题材风格\n");
        prompt.append("8. 输出格式：只输出纯小说正文内容，不包含任何提示、注释、元信息、章节标题、【】标记、（）注释、（本章完）、（注：...）等非小说内容\n");
        prompt.append("9. 内容完整性：直接从故事叙述开始，到悬念结尾结束，不添加任何格式标记或说明文字。禁止添加任何形式的总结、说明或备注\n");
        prompt.append("10. 质量保证：确保输出内容100%为可读小说正文，无任何AI生成痕迹或提示性文字\n");
        prompt.append("11. 故事完结判断：在章节结尾处，通过特殊标记判断故事是否已经自然完结：\n");
        prompt.append("    - 如果故事已经自然完结（所有主要冲突解决，人物命运明确，主题升华完成），在内容末尾添加标记：[STORY_COMPLETE]\n");
        prompt.append("    - 如果故事尚未完结（还有未解决的冲突、未揭开的谜团、未完成的成长），不添加任何标记\n");
        prompt.append("    - 标记格式：[STORY_COMPLETE]（仅此标记，不包含其他内容）\n");

        return prompt.toString();
    }

    /**
     * 获取前两章的内容用于上下文参考
     */
    private List<ArticleChapter> getPreviousChapters(Long articleId, Integer currentChapterNo) {
        if (currentChapterNo <= 1) {
            return new ArrayList<>(); // 第一章不需要前文
        }

        // 查询前两章的内容（如果存在）
        List<ArticleChapter> previousChapters = new ArrayList<>();

        // 查询前一章
        if (currentChapterNo > 1) {
            ArticleChapter prevChapter = articleChapterMapper.selectByArticleIdAndChapterNo(articleId, currentChapterNo - 1);
            if (prevChapter != null && StringUtils.hasText(prevChapter.getChapterContent())) {
                previousChapters.add(prevChapter);
            }
        }

        // 查询前两章
        if (currentChapterNo > 2) {
            ArticleChapter prevPrevChapter = articleChapterMapper.selectByArticleIdAndChapterNo(articleId, currentChapterNo - 2);
            if (prevPrevChapter != null && StringUtils.hasText(prevPrevChapter.getChapterContent())) {
                previousChapters.add(0, prevPrevChapter); // 插入到前面，保持章节顺序
            }
        }

        return previousChapters;
    }

    /**
     * 压缩章节内容为摘要，避免token消耗过多
     * 提取关键情节和人物状态，控制在300字以内
     */
    private String summarizeChapterContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "暂无内容";
        }

        // 如果内容较短，直接返回
        if (content.length() <= 500) {
            return content;
        }

        // 提取前200字和后200字作为摘要
        String start = content.substring(0, Math.min(200, content.length()));
        String end = "";

        if (content.length() > 400) {
            int endStart = Math.max(content.length() - 200, 200);
            end = content.substring(endStart);
        }

        if (end.isEmpty()) {
            return start + "...";
        } else {
            return start + "...[省略中间内容]..." + end;
        }
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

            Integer currentStatus = article.getGenerationStatus() != null ? article.getGenerationStatus() : 0;
            if (allCompleted && currentStatus != 2) {
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



}