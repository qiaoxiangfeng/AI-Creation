package com.aicreation.task;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.entity.po.Plot;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.external.DouBaoClient;
import com.aicreation.external.VolcengineChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 文章章节生成定时任务
 * 根据文章的总字数预估和每章节字数预估生成章节基本信息（不包含内容）
 * 生成内容：章节名称、字数预估、核心剧情、伏笔列表
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
     * 定时任务：每10分钟执行一次文章章节生成
     * 如果上次执行未完成，则丢弃本次调度
     */
    @Scheduled(cron = "20 * * * * ?") // 每10分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void generateArticleChapters() {
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
                    generateChaptersForArticle(article);
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
     * 为单篇文章生成章节基本信息（不包含内容）
     * 根据总字数预估和每章节字数预估计算章节数量
     */
    private void generateChaptersForArticle(Article article) {
        try {
            // 计算章节数量
            Integer totalWordCount = article.getTotalWordCountEstimate();
            Integer chapterWordCount = article.getChapterWordCountEstimate();

            if (totalWordCount == null || chapterWordCount == null || chapterWordCount <= 0) {
                log.warn("文章[{}]字数预估信息不完整，跳过章节生成", article.getArticleName());
                return;
            }

            int totalChapters = Math.max(1, (int) Math.ceil((double) totalWordCount / chapterWordCount));
            log.info("文章[{}]预计生成{}章，总字数{}，每章约{}字", article.getArticleName(), totalChapters, totalWordCount, chapterWordCount);

            // 获取文章的生成配置
            ArticleGenerationConfig config = findArticleGenerationConfig(article);

            // 生成所有章节的基本信息
            ChaptersInfo chaptersInfo = generateChaptersInfo(article, config, totalChapters);

            // 保存章节信息和伏笔信息
            for (int i = 0; i < chaptersInfo.getChapters().size(); i++) {
                ChapterBasicInfo chapterInfo = chaptersInfo.getChapters().get(i);
                int chapterNo = i + 1;

                try {
                    // 检查章节是否已存在
                    ArticleChapter existingChapter = articleChapterMapper.selectByArticleIdAndChapterNo(article.getId(), chapterNo);
                    if (existingChapter != null) {
                        log.info("文章[{}]第{}章已存在，跳过", article.getArticleName(), chapterNo);
                        continue;
                    }

                    // 创建章节记录
                    ArticleChapter chapter = new ArticleChapter();
                    chapter.setArticleId(article.getId());
                    chapter.setChapterNo(chapterNo);
                    chapter.setChapterTitle(chapterInfo.getChapterTitle());
                    chapter.setCorePlot(chapterInfo.getCorePlot());
                    chapter.setWordCountEstimate(chapterInfo.getWordCountEstimate());
                    chapter.setResState(1);
                    chapter.setCreateTime(LocalDateTime.now());
                    chapter.setUpdateTime(LocalDateTime.now());

                    articleChapterMapper.insert(chapter);
                    Long chapterId = chapter.getId();

                    // 保存伏笔信息
                    if (chapterInfo.getPlots() != null && !chapterInfo.getPlots().isEmpty()) {
                        for (PlotInfo plotInfo : chapterInfo.getPlots()) {
                            Plot plot = new Plot();
                            plot.setArticleId(article.getId());
                            plot.setChapterId(chapterId);
                            plot.setPlotName(plotInfo.getPlotName());
                            plot.setPlotContent(plotInfo.getPlotContent());
                            plot.setRecoveryChapterId(plotInfo.getRecoveryChapterId());
                            plot.setResState(1);
                            plot.setCreateTime(LocalDateTime.now());
                            plot.setUpdateTime(LocalDateTime.now());

                            plotMapper.insert(plot);
                        }
                    }

                    log.info("文章[{}]第{}章基本信息生成完成：{}", article.getArticleName(), chapterNo, chapterInfo.getChapterTitle());

                } catch (Exception e) {
                    log.error("保存文章[{}]第{}章信息失败：{}", article.getArticleName(), chapterNo, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("生成文章[{}]章节基本信息失败：{}", article.getArticleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查找文章对应的生成配置
     */
    private ArticleGenerationConfig findArticleGenerationConfig(Article article) {
        try {
            // 根据文章类型查找匹配的配置
            if (StringUtils.hasText(article.getArticleType())) {
                return articleGenerationConfigMapper.selectByTheme(article.getArticleType());
            }
        } catch (Exception e) {
            log.warn("查找文章生成配置失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 生成单个章节的内容
     */
    private ChapterContent generateChapterContent(Article article, ArticleGenerationConfig config, int chapterNo, int totalChapters) {
        // 构建章节生成提示词
        String prompt = buildChapterGenerationPrompt(article, config, chapterNo, totalChapters);

        log.info("=== AI章节生成请求 ===");
        log.info("文章: {}", article.getArticleName());
        log.info("章节: 第{}章", chapterNo);
        log.info("AI提示词: {}", prompt);

        try {
            // 调用豆包AI生成内容
            List<String> results = volcengineChatClient.chatCompletions(
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "doubao-seed-1-6-250615",
                prompt
            );

            log.info("AI响应结果数量: {}", results != null ? results.size() : 0);

            if (results != null && !results.isEmpty()) {
                String generatedContent = results.get(0);
                log.info("=== AI原始响应 ===");
                log.info("AI响应内容: {}", generatedContent);

                ChapterContent parsedContent = parseChapterContent(generatedContent);
                log.info("=== 解析结果 ===");
                log.info("章节标题: {}", parsedContent.getTitle());
                log.info("内容长度: {} 字符", parsedContent.getContent() != null ? parsedContent.getContent().length() : 0);

                return parsedContent;
            } else {
                log.warn("AI返回结果为空");
                throw new RuntimeException("AI返回结果为空");
            }

        } catch (Exception e) {
            log.error("调用AI生成章节内容失败：{}", e.getMessage(), e);
            // 返回默认内容
            return getFallbackChapterContent(article, chapterNo);
        }
    }

    /**
     * 构建章节生成提示词
     */
    private String buildChapterGenerationPrompt(Article article, ArticleGenerationConfig config, int chapterNo, int totalChapters) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("请为小说《%s》生成第%d章的内容。\n\n", article.getArticleName(), chapterNo));

        // 添加文章简介和大纲信息
        if (StringUtils.hasText(article.getArticleOutline())) {
            prompt.append(String.format("小说简介：%s\n\n", article.getArticleOutline()));
        }

        if (StringUtils.hasText(article.getStoryBackground())) {
            prompt.append(String.format("故事背景：%s\n\n", article.getStoryBackground()));
        }

        // 添加生成配置信息
        if (config != null) {
            prompt.append("小说设定：\n");

            if (StringUtils.hasText(config.getGender())) {
                prompt.append(String.format("- 性别定位：%s\n", config.getGender()));
            }
            if (StringUtils.hasText(config.getGenre())) {
                prompt.append(String.format("- 题材类型：%s\n", config.getGenre()));
            }
            if (StringUtils.hasText(config.getPlot())) {
                prompt.append(String.format("- 情节类型：%s\n", config.getPlot()));
            }
            if (StringUtils.hasText(config.getCharacterType())) {
                prompt.append(String.format("- 角色类型：%s\n", config.getCharacterType()));
            }
            if (StringUtils.hasText(config.getStyle())) {
                prompt.append(String.format("- 风格类型：%s\n", config.getStyle()));
            }
            if (StringUtils.hasText(config.getAdditionalCharacteristics())) {
                prompt.append(String.format("- 附加特点：%s\n", config.getAdditionalCharacteristics()));
            }
            prompt.append("\n");
        }

        // 添加章节要求
        prompt.append(String.format("章节要求：\n", chapterNo));
        prompt.append(String.format("1. 本章是第%d章（共%d章）\n", chapterNo, totalChapters));
        prompt.append("2. 章节字数：3000-8000字\n");
        prompt.append("3. 章节标题要吸引人，能概括本章主要内容\n");
        prompt.append("4. 内容要紧凑精彩，节奏明快\n");
        prompt.append("5. 符合小说的整体风格和设定\n");

        // 结尾悬念要求
        prompt.append("6. 【重要】结尾必须留下强烈悬念：\n");
        prompt.append("   - 停在突发危机前\n");
        prompt.append("   - 停在意外反转前\n");
        prompt.append("   - 停在关键抉择前\n");
        prompt.append("   - 最后一句要制造疑问、紧张感或未知威胁\n");
        prompt.append("   - 不交代结果，不做总结，不给出答案\n");
        prompt.append("   - 让读者迫切想看下一章\n\n");

        // 输出格式要求
        prompt.append("请按以下JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"chapterTitle\": \"章节标题\",\n");
        prompt.append("  \"chapterContent\": \"章节正文内容\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 解析章节内容
     */
    private ChapterContent parseChapterContent(String content) {
        ChapterContent result = new ChapterContent();

        log.debug("开始解析章节内容，原始内容长度: {}", content.length());
        log.debug("原始内容预览: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);

        try {
            // 清理内容，去除可能的markdown代码块标记
            String cleanedContent = content.trim();
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7);
            }
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
            }
            cleanedContent = cleanedContent.trim();

            log.debug("清理后的内容: {}", cleanedContent.length() > 200 ? cleanedContent.substring(0, 200) + "..." : cleanedContent);

            // 尝试解析JSON格式
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 配置ObjectMapper允许未转义的控制字符
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(cleanedContent);

            log.debug("JSON解析成功，节点数量: {}", jsonNode.size());

            // 提取章节标题
            if (jsonNode.has("chapterTitle") && jsonNode.get("chapterTitle").isTextual()) {
                result.setTitle(jsonNode.get("chapterTitle").asText());
                log.debug("提取到章节标题: {}", result.getTitle());
            }

            // 提取章节内容
            if (jsonNode.has("chapterContent") && jsonNode.get("chapterContent").isTextual()) {
                result.setContent(jsonNode.get("chapterContent").asText());
                log.debug("提取到章节内容，长度: {}", result.getContent().length());
            }

            // 如果JSON解析失败，尝试回退到旧的字符串解析
            if (!StringUtils.hasText(result.getTitle()) && !StringUtils.hasText(result.getContent())) {
                log.warn("JSON解析未找到有效字段，尝试字符串解析");

                // 解析标题
                int titleStart = content.indexOf("章节标题：[");
                int titleEnd = content.indexOf("]", titleStart);
                if (titleStart >= 0 && titleEnd > titleStart) {
                    result.setTitle(content.substring(titleStart + 6, titleEnd));
                }

                // 解析内容
                int contentStart = content.indexOf("章节正文内容：[");
                int contentEnd = content.indexOf("]", contentStart);
                if (contentStart >= 0 && contentEnd > contentStart) {
                    result.setContent(content.substring(contentStart + 8, contentEnd));
                }
            }

        } catch (Exception e) {
            log.error("解析章节内容失败，使用默认内容。错误详情: {}", e.getMessage());
            log.error("失败的原始内容: {}", content);
            log.error("异常堆栈:", e);

            result.setTitle("AI生成文章 - " + LocalDateTime.now().toString());
            result.setContent(content);
        }

        // 设置默认值
        if (!StringUtils.hasText(result.getTitle())) {
            result.setTitle("第" + LocalDateTime.now().getSecond() + "章");
        }
        if (!StringUtils.hasText(result.getContent())) {
            result.setContent("章节内容生成中...");
        }

        log.info("章节内容解析完成 - 标题: {}, 内容长度: {}",
                 result.getTitle(),
                 result.getContent() != null ? result.getContent().length() : 0);

        return result;
    }

    /**
     * 生成所有章节的基本信息
     */
    private ChaptersInfo generateChaptersInfo(Article article, ArticleGenerationConfig config, int totalChapters) {
        String prompt = buildChaptersInfoPrompt(article, config, totalChapters);

        log.info("=== AI章节基本信息生成请求 ===");
        log.info("文章: {}", article.getArticleName());
        log.info("预计章节数: {}", totalChapters);
        log.info("AI提示词: {}", prompt);

        try {
            List<String> results = volcengineChatClient.chatCompletions(
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "doubao-seed-1-6-250615",
                prompt
            );

            log.info("AI响应结果数量: {}", results != null ? results.size() : 0);

            if (results != null && !results.isEmpty()) {
                String generatedContent = results.get(0);
                log.info("=== AI原始响应 ===");
                log.info("AI响应内容: {}", generatedContent);

                ChaptersInfo parsedInfo = parseChaptersInfo(generatedContent, totalChapters);
                log.info("=== 解析结果 ===");
                log.info("成功解析章节数: {}", parsedInfo.getChapters().size());

                return parsedInfo;
            } else {
                log.warn("AI返回结果为空");
                throw new RuntimeException("AI返回结果为空");
            }
        } catch (Exception e) {
            log.error("调用AI生成章节基本信息失败：{}", e.getMessage(), e);
            return getFallbackChaptersInfo(article, totalChapters);
        }
    }

    /**
     * 构建章节基本信息生成的AI提示词
     */
    private String buildChaptersInfoPrompt(Article article, ArticleGenerationConfig config, int totalChapters) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为小说《").append(article.getArticleName()).append("》生成章节基本信息。\n\n");

        // 添加文章基本信息
        if (StringUtils.hasText(article.getArticleOutline())) {
            prompt.append("故事大纲：\n").append(article.getArticleOutline()).append("\n\n");
        }

        // 添加生成配置信息
        if (config != null) {
            if (StringUtils.hasText(config.getGender())) {
                prompt.append("受众性别：").append(config.getGender()).append("\n");
            }
            if (StringUtils.hasText(config.getGenre())) {
                prompt.append("题材类型：").append(config.getGenre()).append("\n");
            }
            if (StringUtils.hasText(config.getPlot())) {
                prompt.append("情节类型：").append(config.getPlot()).append("\n");
            }
            if (StringUtils.hasText(config.getStyle())) {
                prompt.append("写作风格：").append(config.getStyle()).append("\n");
            }
            if (StringUtils.hasText(config.getCharacterType())) {
                prompt.append("角色类型：").append(config.getCharacterType()).append("\n");
            }
        }

        prompt.append("\n预计总章节数：").append(totalChapters).append("章\n");
        prompt.append("每章预估字数：").append(article.getChapterWordCountEstimate()).append("字\n\n");

        prompt.append("请生成JSON格式的输出，包含以下内容：\n");
        prompt.append("1. 章节名称\n");
        prompt.append("2. 核心剧情（本章要发生什么，主角性格目标能力情绪，重要配角状态）\n");
        prompt.append("3. 字数预估\n");
        prompt.append("4. 伏笔列表（伏笔名称、伏笔内容、回收章节号）\n\n");

        prompt.append("输出格式示例：\n");
        prompt.append("{\n");
        prompt.append("  \"chapters\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"chapterTitle\": \"章节名称\",\n");
        prompt.append("      \"corePlot\": \"核心剧情描述\",\n");
        prompt.append("      \"wordCountEstimate\": 4000,\n");
        prompt.append("      \"plots\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"plotName\": \"伏笔名称\",\n");
        prompt.append("          \"plotContent\": \"伏笔内容\",\n");
        prompt.append("          \"recoveryChapterId\": 5\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * 解析AI返回的章节信息
     */
    private ChaptersInfo parseChaptersInfo(String content, int expectedChapters) {
        ChaptersInfo result = new ChaptersInfo();

        try {
            String cleanedContent = content.trim();
            // 移除可能的markdown代码块标记
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7);
            }
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
            }
            cleanedContent = cleanedContent.trim();

            // 使用Jackson解析JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(cleanedContent);

            if (rootNode.has("chapters")) {
                com.fasterxml.jackson.databind.JsonNode chaptersNode = rootNode.get("chapters");
                if (chaptersNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode chapterNode : chaptersNode) {
                        ChapterBasicInfo chapterInfo = new ChapterBasicInfo();

                        if (chapterNode.has("chapterTitle")) {
                            chapterInfo.setChapterTitle(chapterNode.get("chapterTitle").asText());
                        }
                        if (chapterNode.has("corePlot")) {
                            chapterInfo.setCorePlot(chapterNode.get("corePlot").asText());
                        }
                        if (chapterNode.has("wordCountEstimate")) {
                            chapterInfo.setWordCountEstimate(chapterNode.get("wordCountEstimate").asInt());
                        }

                        // 解析伏笔信息
                        if (chapterNode.has("plots") && chapterNode.get("plots").isArray()) {
                            List<PlotInfo> plots = new ArrayList<>();
                            for (com.fasterxml.jackson.databind.JsonNode plotNode : chapterNode.get("plots")) {
                                PlotInfo plotInfo = new PlotInfo();
                                if (plotNode.has("plotName")) {
                                    plotInfo.setPlotName(plotNode.get("plotName").asText());
                                }
                                if (plotNode.has("plotContent")) {
                                    plotInfo.setPlotContent(plotNode.get("plotContent").asText());
                                }
                                if (plotNode.has("recoveryChapterId")) {
                                    plotInfo.setRecoveryChapterId(plotNode.get("recoveryChapterId").asLong());
                                }
                                plots.add(plotInfo);
                            }
                            chapterInfo.setPlots(plots);
                        }

                        result.addChapter(chapterInfo);
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析章节信息失败：{}", e.getMessage(), e);
        }

        // 如果解析失败或章节数不够，返回默认信息
        if (result.getChapters().isEmpty() || result.getChapters().size() < expectedChapters) {
            log.warn("AI返回的章节信息不完整，使用默认信息");
            return getFallbackChaptersInfo(null, expectedChapters);
        }

        return result;
    }

    /**
     * 获取默认章节信息（当AI生成失败时使用）
     */
    private ChaptersInfo getFallbackChaptersInfo(Article article, int totalChapters) {
        ChaptersInfo result = new ChaptersInfo();

        for (int i = 1; i <= totalChapters; i++) {
            ChapterBasicInfo chapterInfo = new ChapterBasicInfo();
            chapterInfo.setChapterTitle("第" + i + "章");
            chapterInfo.setCorePlot("主角在这一章中继续冒险，面对各种挑战，展现出成长。");
            chapterInfo.setWordCountEstimate(4000);
            chapterInfo.setPlots(new ArrayList<>());
            result.addChapter(chapterInfo);
        }

        return result;
    }

    /**
     * 获取默认章节内容
     */
    private ChapterContent getFallbackChapterContent(Article article, int chapterNo) {
        ChapterContent content = new ChapterContent();
        content.setTitle("第" + chapterNo + "章 新的开始");
        content.setContent("故事正在精彩展开，敬请期待下一章的内容。");
        return content;
    }

    /**
     * 章节内容内部类
     */
    private static class ChapterContent {
        private String title;
        private String content;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 章节基本信息
     */
    private static class ChapterBasicInfo {
        private String chapterTitle;
        private String corePlot;
        private Integer wordCountEstimate;
        private List<PlotInfo> plots;

        // Getters and Setters
        public String getChapterTitle() { return chapterTitle; }
        public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }

        public String getCorePlot() { return corePlot; }
        public void setCorePlot(String corePlot) { this.corePlot = corePlot; }

        public Integer getWordCountEstimate() { return wordCountEstimate; }
        public void setWordCountEstimate(Integer wordCountEstimate) { this.wordCountEstimate = wordCountEstimate; }

        public List<PlotInfo> getPlots() { return plots; }
        public void setPlots(List<PlotInfo> plots) { this.plots = plots; }
    }

    /**
     * 伏笔信息
     */
    private static class PlotInfo {
        private String plotName;
        private String plotContent;
        private Long recoveryChapterId;

        // Getters and Setters
        public String getPlotName() { return plotName; }
        public void setPlotName(String plotName) { this.plotName = plotName; }

        public String getPlotContent() { return plotContent; }
        public void setPlotContent(String plotContent) { this.plotContent = plotContent; }

        public Long getRecoveryChapterId() { return recoveryChapterId; }
        public void setRecoveryChapterId(Long recoveryChapterId) { this.recoveryChapterId = recoveryChapterId; }
    }

    /**
     * 所有章节信息
     */
    private static class ChaptersInfo {
        private List<ChapterBasicInfo> chapters;

        public ChaptersInfo() {
            this.chapters = new ArrayList<>();
        }

        public List<ChapterBasicInfo> getChapters() { return chapters; }
        public void setChapters(List<ChapterBasicInfo> chapters) { this.chapters = chapters; }
        public void addChapter(ChapterBasicInfo chapter) { this.chapters.add(chapter); }
    }
}