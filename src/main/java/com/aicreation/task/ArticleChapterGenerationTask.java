package com.aicreation.task;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
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

/**
 * 文章章节生成定时任务
 * 根据文章标题和大纲生成章节内容
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
            // 查询需要生成章节的文章（content_generated = 0）
            List<Article> articlesToProcess = articleMapper.selectArticlesWithoutContent();

            if (articlesToProcess == null || articlesToProcess.isEmpty()) {
                log.info("没有需要生成章节的文章");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Article article : articlesToProcess) {
                try {
                    generateChaptersForArticle(article);
                    // 标记文章内容已生成
                    article.setContentGenerated(1);
                    article.setUpdateTime(LocalDateTime.now());
                    articleMapper.updateByPrimaryKey(article);
                    successCount++;

                    log.info("文章[{}]章节生成完成", article.getArticleName());

                    // 添加短暂延迟，避免API调用过于频繁
                    Thread.sleep(2000);

                } catch (Exception e) {
                    log.error("生成文章[{}]章节失败：{}", article.getArticleName(), e.getMessage());
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
     * 为单篇文章生成章节
     */
    private void generateChaptersForArticle(Article article) {
        // 根据文章类型确定章节数量（这里假设生成10章）
        int totalChapters = 10;

        // 获取文章的生成配置（如果有关联的话）
        ArticleGenerationConfig config = findArticleGenerationConfig(article);

        for (int i = 1; i <= totalChapters; i++) {
            try {
                // 检查章节是否已存在
                ArticleChapter existingChapter = articleChapterMapper.selectByArticleIdAndChapterNo(article.getId(), i);
                if (existingChapter != null) {
                    log.info("文章[{}]第{}章已存在，跳过", article.getArticleName(), i);
                    continue;
                }

                // 生成章节内容
                ChapterContent chapterContent = generateChapterContent(article, config, i, totalChapters);

                // 创建章节记录
                ArticleChapter chapter = new ArticleChapter();
                chapter.setArticleId(article.getId());
                chapter.setChapterNo(i);
                chapter.setChapterTitle(chapterContent.getTitle());
                chapter.setChapterContent(chapterContent.getContent());
                chapter.setResState(1);
                chapter.setCreateTime(LocalDateTime.now());
                chapter.setUpdateTime(LocalDateTime.now());

                articleChapterMapper.insert(chapter);

                log.info("文章[{}]第{}章生成完成：{}", article.getArticleName(), i, chapterContent.getTitle());

                // 添加延迟
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("生成文章[{}]第{}章失败：{}", article.getArticleName(), i, e.getMessage());
                // 继续处理下一章
            }
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
}