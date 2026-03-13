package com.aicreation.generate;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.entity.po.Plot;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.external.ResponseContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章内容生成器
 * 提供统一的文章章节内容生成接口
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
@Service
public class ArticleContentGenerator {

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
     * 为指定章节生成内容
     * 此方法会立即更新数据库，确保内容实时落表
     * 无事务保护，数据实时提交
     *
     * @param chapterId 章节ID
     */
    public void generateChapterContent(Long chapterId) {
        ArticleChapter chapter = articleChapterMapper.selectByPrimaryKey(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在: " + chapterId);
        }

        generateContentForChapter(chapter);
    }

    /**
     * 根据用户修改意见重新生成指定章节的内容
     *
     * @param chapterId   章节ID
     * @param instruction 用户对本章节内容的修改意见
     */
    public void regenerateChapterContent(Long chapterId, String instruction) {
        ArticleChapter chapter = articleChapterMapper.selectByPrimaryKey(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在: " + chapterId);
        }

        // 获取文章信息
        Article article = articleMapper.selectByPrimaryKey(chapter.getArticleId());
        if (article == null) {
            throw new RuntimeException("文章不存在: " + chapter.getArticleId());
        }

        log.info("开始重新生成文章[{}]第{}章的内容，章节ID: {}", article.getArticleName(), chapter.getChapterNo(), chapterId);

        // 获取文章生成配置
        ArticleGenerationConfig config = findArticleGenerationConfig(article);

        // 获取本章的伏笔信息
        List<Plot> chapterPlots = plotMapper.selectByChapterId(chapter.getId());

        // 获取需要在本章回收的伏笔
        List<Plot> recoveryPlots = new ArrayList<>();
        if (chapterPlots != null) {
            for (Plot plot : chapterPlots) {
                if (plot.getRecoveryChapterId() != null &&
                    plot.getRecoveryChapterId().equals(chapter.getId())) {
                    recoveryPlots.add(plot);
                }
            }
        }

        // 基础提示词
        String basePrompt = buildChapterContentPrompt(article, chapter, config, chapterPlots, recoveryPlots);

        // 将用户修改意见附加到提示词后面
        StringBuilder promptBuilder = new StringBuilder(basePrompt);
        promptBuilder.append("\n\n用户对上一版章节内容的修改意见如下，请在保留整体剧情合理性的前提下，重点根据这些意见重写或调整本章内容：\n");
        promptBuilder.append(instruction).append("\n");

        String prompt = promptBuilder.toString();

        log.info("=== AI章节内容重新生成请求 ===");
        log.info("文章: {}, 章节: 第{}章", article.getArticleName(), chapter.getChapterNo());
        log.info("AI提示词长度: {} 字符", prompt.length());
        log.debug("=== 重新生成提示词 ===");
        log.debug("{}", prompt);

        try {
            // 优先使用上一轮正文生成的 response_id，如果不存在则退回到章节规划时的 response_id
            String previousResponseId = StringUtils.hasText(chapter.getResponseIdContent())
                    ? chapter.getResponseIdContent()
                    : chapter.getResponseIdPlan();

            log.info("本次AI章节内容重新生成请求 previous_response_id: {}", previousResponseId != null ? previousResponseId : "null");
            com.volcengine.ark.runtime.model.responses.response.ResponseObject response =
                    volcengineChatClient.createResponse(prompt, previousResponseId, "content");
            String generatedContent = ResponseContentExtractor.extractContent(response);

            log.info("=== AI章节内容重新生成响应 ===");
            log.info("AI返回的response_id: {}", response.getId());
            log.info("生成内容长度: {} 字符", generatedContent.length());

            if (!StringUtils.hasText(generatedContent)) {
                throw new RuntimeException("AI返回的内容为空");
            }

            // 更新章节内容与本次正文 response_id
            chapter.setChapterContent(generatedContent);
            chapter.setResponseIdContent(response.getId());
            chapter.setGenerationStatus(2);
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);

            log.info("文章[{}]第{}章内容重新生成并更新成功", article.getArticleName(), chapter.getChapterNo());

        } catch (Exception e) {
            log.error("重新生成文章[{}]第{}章内容失败：{}", article.getArticleName(), chapter.getChapterNo(), e.getMessage(), e);
            throw new RuntimeException("重新生成章节内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为章节生成内容的具体实现
     */
    private void generateContentForChapter(ArticleChapter chapter) {
        // 获取文章信息
        Article article = articleMapper.selectByPrimaryKey(chapter.getArticleId());
        if (article == null) {
            throw new RuntimeException("文章不存在: " + chapter.getArticleId());
        }

        // 仅当章节生成已全部完成（故事完结）时，才允许触发章节内容生成
        if (!Boolean.TRUE.equals(article.getStoryComplete())) {
            throw new RuntimeException("章节尚未全部生成完成（故事未完结），请先完成章节生成后再生成章节内容");
        }

        // 检查章节是否已经有内容
        if (StringUtils.hasText(chapter.getChapterContent())) {
            log.info("章节[{}]已有内容，跳过生成", chapter.getChapterNo());
            return;
        }

        // 检查章节是否正在生成中
        if (chapter.getGenerationStatus() != null && chapter.getGenerationStatus() == 1) {
            log.info("章节[{}]正在生成中，跳过", chapter.getChapterNo());
            return;
        }

        log.info("开始生成文章[{}]第{}章的内容", article.getArticleName(), chapter.getChapterNo());

        try {
            // 更新章节状态为生成中
            chapter.setGenerationStatus(1); // 生成中
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);

            // 获取文章生成配置
            ArticleGenerationConfig config = findArticleGenerationConfig(article);

            // 获取本章的伏笔信息
            List<Plot> chapterPlots = plotMapper.selectByChapterId(chapter.getId());

            // 获取需要在本章回收的伏笔
            List<Plot> recoveryPlots = new ArrayList<>();
            if (chapterPlots != null) {
                for (Plot plot : chapterPlots) {
                    if (plot.getRecoveryChapterId() != null &&
                        plot.getRecoveryChapterId().equals(chapter.getId())) {
                        recoveryPlots.add(plot);
                    }
                }
            }

            // 生成章节内容
            String chapterContent = generateChapterContent(article, chapter, config, chapterPlots, recoveryPlots);

            // 更新章节内容和状态
            chapter.setChapterContent(chapterContent);
            chapter.setGenerationStatus(2); // 已完成
            chapter.setUpdateTime(LocalDateTime.now());
            int updateResult = articleChapterMapper.updateByPrimaryKey(chapter);
            if (updateResult == 0) {
                log.error("更新章节内容失败，章节ID: {}", chapter.getId());
                throw new RuntimeException("章节内容更新失败");
            }
            log.debug("文章[{}]第{}章内容更新成功", article.getArticleName(), chapter.getChapterNo());

            // 检查并更新文章完成状态
            // story_complete 仅用于“章节生成是否完结”的标识，不在内容生成流程中更新

            log.info("文章[{}]第{}章内容生成完成，字数：{}",
                    article.getArticleName(), chapter.getChapterNo(),
                    chapterContent != null ? chapterContent.length() : 0);

        } catch (Exception e) {
            log.error("生成文章[{}]第{}章内容失败：{}", article.getArticleName(), chapter.getChapterNo(), e.getMessage(), e);

            // 更新章节状态为生成失败
            chapter.setGenerationStatus(3); // 生成失败
            chapter.setUpdateTime(LocalDateTime.now());
            try {
                articleChapterMapper.updateByPrimaryKey(chapter);
                log.debug("文章[{}]第{}章状态更新为失败", article.getArticleName(), chapter.getChapterNo());
            } catch (Exception updateException) {
                log.error("更新章节失败状态时出错：{}", updateException.getMessage());
                // 不抛出异常，避免覆盖原始异常
            }

            throw new RuntimeException("生成章节内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成章节内容的具体实现
     */
    private String generateChapterContent(Article article, ArticleChapter chapter,
                                        ArticleGenerationConfig config,
                                        List<Plot> chapterPlots, List<Plot> recoveryPlots) {
        String prompt = buildChapterContentPrompt(article, chapter, config, chapterPlots, recoveryPlots);

        log.info("=== AI章节内容生成请求 ===");
        log.info("文章: {}, 章节: 第{}章", article.getArticleName(), chapter.getChapterNo());
        log.info("AI提示词长度: {} 字符", prompt.length());
        log.debug("=== 完整AI提示词 ===");
        log.debug("{}", prompt);

        try {
            // 使用Responses API生成章节内容，以上一轮本章规划的 response_id 作为上下文起点
            log.info("开始使用Responses API生成章节内容...");
            String previousResponseId = chapter.getResponseIdPlan();
            log.info("本次AI章节内容生成请求 previous_response_id: {}", previousResponseId != null ? previousResponseId : "null");
            com.volcengine.ark.runtime.model.responses.response.ResponseObject response =
                    volcengineChatClient.createResponse(prompt, previousResponseId, "content");
            String generatedContent = ResponseContentExtractor.extractContent(response);

            log.info("=== AI原始响应 ===");
            log.info("AI返回的response_id: {}", response.getId());
            log.info("生成内容长度: {} 字符", generatedContent.length());

            // 记录本次正文生成使用的 response_id，便于后续重新生成时复用上下文
            chapter.setResponseIdContent(response.getId());

            // 验证生成的内容是否为空
            if (!StringUtils.hasText(generatedContent)) {
                throw new RuntimeException("AI返回的内容为空");
            }

            return generatedContent;

        } catch (Exception e) {
            log.error("调用AI生成章节内容失败：{}", e.getMessage(), e);
            throw new RuntimeException("AI生成章节内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找文章对应的生成配置
     */
    private ArticleGenerationConfig findArticleGenerationConfig(Article article) {
        try {
            // 根据文章类型查找匹配的配置
            if (article.getArticleType() != null && !article.getArticleType().trim().isEmpty()) {
                return articleGenerationConfigMapper.selectByTheme(article.getArticleType());
            }
        } catch (Exception e) {
            log.warn("查找文章生成配置失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 构建章节内容生成提示词
     */
    private String buildChapterContentPrompt(Article article, ArticleChapter chapter,
                                           ArticleGenerationConfig config,
                                           List<Plot> chapterPlots, List<Plot> recoveryPlots) {
        StringBuilder prompt = new StringBuilder();

        // 文章基本信息
        prompt.append("请为小说《").append(article.getArticleName()).append("》生成第").append(chapter.getChapterNo()).append("章的完整内容。\n\n");

        // 章节基本信息（仅保留本章特有信息，避免重复的全局配置）
        prompt.append("第").append(chapter.getChapterNo()).append("章标题：").append(chapter.getChapterTitle()).append("\n");
        if (StringUtils.hasText(chapter.getCorePlot())) {
            prompt.append("核心情节：").append(chapter.getCorePlot()).append("\n");
        }
        prompt.append("预估字数：").append(chapter.getWordCountEstimate() != null ? chapter.getWordCountEstimate() : 2000).append("字\n\n");

        // 伏笔信息（与上下文不同步的本章局部信息）
        if ((chapterPlots != null && !chapterPlots.isEmpty()) ||
            (recoveryPlots != null && !recoveryPlots.isEmpty())) {
            prompt.append("\n剧情伏笔信息：\n");

            // 本章设置的伏笔
            if (chapterPlots != null && !chapterPlots.isEmpty()) {
                prompt.append("本章设置的伏笔：\n");
                for (Plot plot : chapterPlots) {
                    prompt.append("- ").append(plot.getPlotName()).append("：").append(plot.getPlotContent()).append("\n");
                }
            }

            // 本章需要回收的伏笔
            if (recoveryPlots != null && !recoveryPlots.isEmpty()) {
                prompt.append("本章需要回收的伏笔：\n");
                for (Plot plot : recoveryPlots) {
                    prompt.append("- ").append(plot.getPlotName()).append("：").append(plot.getPlotContent()).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 简要生成指令（避免重复罗列通用写作规范）
        prompt.append("请根据以上信息，生成本章的小说正文内容，要求：\n");
        prompt.append("1. 使用连续、自然的小说段落，不要使用 Markdown 或其它形式的小标题，例如“### 一、xxx”之类。\n");
        prompt.append("2. 不要在正文中输出“本章完”、“（本章完）”、“(本章完)”或类似章节结束标记。\n");
        prompt.append("3. 不要输出任何结构说明、分节标题，只保留读者阅读用的纯正文。\n");

        return prompt.toString();
    }
}