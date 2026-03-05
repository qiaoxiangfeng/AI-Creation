package com.aicreation.generate;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.Plot;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.service.ResponseIdManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文章章节生成器
 * 提供统一的文章章节生成接口
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
@Service
public class ArticleChapterGenerator {

    @Autowired
    private ArticleChapterMapper articleChapterMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private PlotMapper plotMapper;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    @Autowired
    private ResponseIdManager responseIdManager;

    /**
     * 为指定文章生成章节
     *
     * @param articleId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateChaptersForArticle(Long articleId) {
        // 获取文章信息
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + articleId);
        }


        log.info("开始为文章[{}]生成章节", article.getArticleName());

        // 验证字数预估信息（可选，用于日志记录）
        Integer totalWordCount = article.getTotalWordCountEstimate();
        Integer chapterWordCount = article.getChapterWordCountEstimate();

        log.info("文章[{}]开始动态生成章节，总字数预估{}，每章约{}字",
                article.getArticleName(),
                totalWordCount != null ? totalWordCount : "未知",
                chapterWordCount != null ? chapterWordCount : "未知");

        int currentChapterNo = 1;
        int generatedCount = 0;
        boolean storyComplete = false;

        // 设置安全上限，防止无限循环（最大100章）
        final int MAX_CHAPTERS = 100;

        // 逐章生成，直到故事完结
        while (!storyComplete && currentChapterNo <= MAX_CHAPTERS) {
            log.info("开始生成文章[{}]第{}章", article.getArticleName(), currentChapterNo);

            try {
                // 检查章节是否已存在
                ArticleChapter existingChapter = articleChapterMapper.selectByArticleIdAndChapterNo(article.getId(), currentChapterNo);
                if (existingChapter != null) {
                    log.info("文章[{}]第{}章已存在，跳过", article.getArticleName(), currentChapterNo);
                    currentChapterNo++;
                    continue;
                }

                // 生成单个章节的基本信息（包含完结判断）
                ChapterWithCompleteInfo chapterInfo =
                    generateSingleChapterWithCompleteCheck(article, currentChapterNo);

                // 检查故事是否完结
                storyComplete = chapterInfo.isStoryComplete();
                log.info("文章[{}]第{}章生成完成，故事完结标志: {}", article.getArticleName(), currentChapterNo, storyComplete);

                // 创建章节记录
                ArticleChapter chapter = new ArticleChapter();
                chapter.setArticleId(article.getId());
                chapter.setChapterNo(currentChapterNo);
                chapter.setChapterTitle(chapterInfo.getChapterTitle());
                chapter.setCorePlot(chapterInfo.getCorePlot());
                chapter.setWordCountEstimate(chapterInfo.getWordCountEstimate());
                chapter.setStoryComplete(storyComplete); // 设置完结标志
                chapter.setResState(1);
                chapter.setCreateTime(LocalDateTime.now());
                chapter.setUpdateTime(LocalDateTime.now());

                articleChapterMapper.insert(chapter);
                generatedCount++;

                    // 处理章节的伏笔信息
                    if (chapterInfo.getPlots() != null && !chapterInfo.getPlots().isEmpty()) {
                        int plotCount = 0;
                        for (PlotInfo plotInfo : chapterInfo.getPlots()) {
                        Plot plot = new Plot();
                        plot.setArticleId(article.getId());
                        plot.setChapterId(chapter.getId());
                        plot.setPlotName(plotInfo.getPlotName());
                        plot.setPlotContent(plotInfo.getPlotContent());
                        plot.setRecoveryChapterId(plotInfo.getRecoveryChapterId());
                        plot.setResState(1);
                        plot.setCreateTime(LocalDateTime.now());
                        plot.setUpdateTime(LocalDateTime.now());

                        plotMapper.insert(plot);
                        plotCount++;
                    }
                    if (plotCount > 0) {
                        log.info("文章[{}]第{}章保存了{}个伏笔", article.getArticleName(), currentChapterNo, plotCount);
                    }
                }

                currentChapterNo++;

                // 如果不是故事最后，稍微暂停一下，避免对AI服务造成过大压力
                if (!storyComplete) {
                    try {
                        Thread.sleep(1000); // 暂停1秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                log.error("生成文章[{}]第{}章失败: {}", article.getArticleName(), currentChapterNo, e.getMessage(), e);
                // 章节生成失败，停止本篇文章的生成任务
                throw new RuntimeException("章节生成失败，停止文章《" + article.getArticleName() + "》的生成任务: " + e.getMessage(), e);
            }
        }

        log.info("文章[{}]章节基本信息生成完成，共生成{}章，故事{}完结",
                article.getArticleName(), generatedCount, storyComplete ? "已" : "未");
    }

    /**
     * 生成单个章节的基本信息（包含完结判断）
     */
    public ChapterWithCompleteInfo generateSingleChapterWithCompleteCheck(Article article, int chapterNo) {
        String prompt = buildSingleChapterWithCompletePrompt(article, chapterNo);

        log.info("=== AI单章完结判断请求 ===");
        log.info("文章: {}", article.getArticleName());
        log.info("章节: 第{}章", chapterNo);
        log.info("AI提示词长度: {} 字符", prompt.length());
        log.debug("=== 完整AI提示词 ===");
        log.debug("{}", prompt);

        try {
            // 使用Responses API生成章节信息
            log.info("开始使用Responses API生成单章信息...");
            String generatedContent = responseIdManager.callAIWithResponsesAPI(article, prompt);

            log.info("=== AI原始响应 ===");
            log.info("AI响应内容: {}", generatedContent);

            ChapterWithCompleteInfo parsedInfo = parseSingleChapterWithComplete(generatedContent, chapterNo);
            log.info("=== 解析结果 ===");
            log.info("章节标题: {}", parsedInfo.getChapterTitle());
            log.info("故事完结: {}", parsedInfo.isStoryComplete());
            log.info("字数预估: {}", parsedInfo.getWordCountEstimate());

            return parsedInfo;

        } catch (Exception e) {
            log.error("调用AI生成单章信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("AI生成章节信息失败: " + e.getMessage(), e);
        }
    }


    /**
     * 构建单个章节生成提示词（包含完结判断）
     */
    private String buildSingleChapterWithCompletePrompt(Article article, int chapterNo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下小说生成第").append(chapterNo).append("章的基本信息：\n\n");

        // 文章基本信息
        prompt.append("小说名称：").append(article.getArticleName()).append("\n");
        if (StringUtils.hasText(article.getArticleOutline())) {
            prompt.append("小说大纲：").append(article.getArticleOutline()).append("\n");
        }
        if (StringUtils.hasText(article.getStoryBackground())) {
            prompt.append("故事背景：").append(article.getStoryBackground()).append("\n");
        }
        if (StringUtils.hasText(article.getArticleType())) {
            prompt.append("小说类型：").append(article.getArticleType()).append("\n");
        }

        prompt.append("\n请生成第").append(chapterNo).append("章的以下信息：\n");
        prompt.append("1. chapterTitle：章节标题（简洁有力，吸引读者，避免在标题中使用引号）\n");
        prompt.append("2. corePlot：核心情节概述（200-300字，包含主要冲突和转折）\n");
        prompt.append("3. wordCountEstimate：预估字数（").append(article.getChapterWordCountEstimate() != null ? article.getChapterWordCountEstimate() : 2000).append("字左右）\n");
        prompt.append("4. storyComplete：故事是否完结（true/false）\n");
        prompt.append("5. plots：本章设置的伏笔（可选，数组格式，每个伏笔包含plotName和plotContent）\n");

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他内容：\n");
        prompt.append("{\n");
        prompt.append("  \"chapterTitle\": \"章节标题\",\n");
        prompt.append("  \"corePlot\": \"核心情节概述\",\n");
        prompt.append("  \"wordCountEstimate\": 预估字数,\n");
        prompt.append("  \"storyComplete\": false,\n");
        prompt.append("  \"plots\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"plotName\": \"伏笔名称\",\n");
        prompt.append("      \"plotContent\": \"伏笔内容描述\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("注意事项：\n");
        prompt.append("- 如果这是故事的自然结局，设置为true\n");
        prompt.append("- 如果故事还有发展空间，设置为false\n");
        prompt.append("- 字数预估(totalWordCountEstimate)和每章字数(chapterWordCountEstimate)仅供参考，AI可以根据故事需要调整\n");

        return prompt.toString();
    }

    /**
     * 解析单个章节信息（包含完结判断）
     */
    private ChapterWithCompleteInfo parseSingleChapterWithComplete(String content, int chapterNo) {
        try {
            // 直接解析JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> jsonMap = objectMapper.readValue(content, java.util.Map.class);

            ChapterWithCompleteInfo result = new ChapterWithCompleteInfo();
            result.setChapterTitle((String) jsonMap.get("chapterTitle"));
            result.setCorePlot((String) jsonMap.get("corePlot"));

            Object wordCountObj = jsonMap.get("wordCountEstimate");
            if (wordCountObj instanceof Number) {
                result.setWordCountEstimate(((Number) wordCountObj).intValue());
            }

            Object storyCompleteObj = jsonMap.get("storyComplete");
            if (storyCompleteObj instanceof Boolean) {
                result.setStoryComplete((Boolean) storyCompleteObj);
            }

            // 解析伏笔信息
            Object plotsObj = jsonMap.get("plots");
            if (plotsObj instanceof List) {
                List<java.util.Map<String, Object>> plotsList = (List<java.util.Map<String, Object>>) plotsObj;
                List<PlotInfo> plots = new java.util.ArrayList<>();
                for (java.util.Map<String, Object> plotMap : plotsList) {
                    PlotInfo plotInfo = new PlotInfo();
                    plotInfo.setPlotName((String) plotMap.get("plotName"));
                    plotInfo.setPlotContent((String) plotMap.get("plotContent"));
                    plots.add(plotInfo);
                }
                result.setPlots(plots);
            }

            log.info("单章信息解析完成 - 标题: {}, 完结: {}, 字数: {}, 伏笔数: {}",
                     result.getChapterTitle(),
                     result.isStoryComplete(),
                     result.getWordCountEstimate(),
                     result.getPlots() != null ? result.getPlots().size() : 0);

            return result;

        } catch (Exception e) {
            log.error("解析单章信息失败，尝试清理JSON后重试: {}", e.getMessage());
            try {
                // 尝试清理JSON内容
                String cleanedContent = cleanJsonContent(content);
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> jsonMap = objectMapper.readValue(cleanedContent, java.util.Map.class);

                ChapterWithCompleteInfo result = new ChapterWithCompleteInfo();
                result.setChapterTitle((String) jsonMap.get("chapterTitle"));
                result.setCorePlot((String) jsonMap.get("corePlot"));

                Object wordCountObj = jsonMap.get("wordCountEstimate");
                if (wordCountObj instanceof Number) {
                    result.setWordCountEstimate(((Number) wordCountObj).intValue());
                }

                Object storyCompleteObj = jsonMap.get("storyComplete");
                if (storyCompleteObj instanceof Boolean) {
                    result.setStoryComplete((Boolean) storyCompleteObj);
                }

                // 解析伏笔信息
                Object plotsObj = jsonMap.get("plots");
                if (plotsObj instanceof List) {
                    List<java.util.Map<String, Object>> plotsList = (List<java.util.Map<String, Object>>) plotsObj;
                    List<PlotInfo> plots = new java.util.ArrayList<>();
                    for (java.util.Map<String, Object> plotMap : plotsList) {
                        PlotInfo plotInfo = new PlotInfo();
                        plotInfo.setPlotName((String) plotMap.get("plotName"));
                        plotInfo.setPlotContent((String) plotMap.get("plotContent"));
                        plots.add(plotInfo);
                    }
                    result.setPlots(plots);
                }

                log.info("清理后解析成功 - 标题: {}, 完结: {}, 字数: {}, 伏笔数: {}",
                         result.getChapterTitle(),
                         result.isStoryComplete(),
                         result.getWordCountEstimate(),
                         result.getPlots() != null ? result.getPlots().size() : 0);

                return result;

            } catch (Exception cleanException) {
                log.error("清理后仍解析失败: {}", cleanException.getMessage(), cleanException);
                throw new RuntimeException("AI返回格式错误，无法解析章节信息: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 清理JSON内容，修复常见的格式问题
     */
    private String cleanJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        StringBuilder cleaned = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                cleaned.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                cleaned.append(c);
                continue;
            }

            if (c == '"') {
                if (inString) {
                    // 检查下一个字符是否是字符串结束符（, ] } 或字符串结束）
                    if (i + 1 < content.length()) {
                        char next = content.charAt(i + 1);
                        if (next == ',' || next == ']' || next == '}' || Character.isWhitespace(next)) {
                            // 这是一个正确的字符串结束
                            inString = false;
                            cleaned.append(c);
                        } else {
                            // 这是一个未转义的引号，需要转义
                            cleaned.append("\\\"");
                        }
                    } else {
                        // 字符串结束
                        inString = false;
                        cleaned.append(c);
                    }
                } else {
                    // 字符串开始
                    inString = true;
                    cleaned.append(c);
                }
            } else {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }

    /**
     * 包含完结判断的章节信息
     */
    public static class ChapterWithCompleteInfo extends ChapterBasicInfo {
        private boolean storyComplete;

        public boolean isStoryComplete() { return storyComplete; }
        public void setStoryComplete(boolean storyComplete) { this.storyComplete = storyComplete; }
    }

    /**
     * 章节基本信息
     */
    public static class ChapterBasicInfo {
        private String chapterTitle;
        private String corePlot;
        private Integer wordCountEstimate;
        private List<PlotInfo> plots;

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
    public static class PlotInfo {
        private String plotName;
        private String plotContent;
        private Long recoveryChapterId;

        public String getPlotName() { return plotName; }
        public void setPlotName(String plotName) { this.plotName = plotName; }

        public String getPlotContent() { return plotContent; }
        public void setPlotContent(String plotContent) { this.plotContent = plotContent; }

        public Long getRecoveryChapterId() { return recoveryChapterId; }
        public void setRecoveryChapterId(Long recoveryChapterId) { this.recoveryChapterId = recoveryChapterId; }
    }
}