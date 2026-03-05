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
import com.aicreation.service.ResponseIdManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private ResponseIdManager responseIdManager;

    /**
     * 为指定章节生成内容
     *
     * @param chapterId 章节ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateChapterContent(Long chapterId) {
        ArticleChapter chapter = articleChapterMapper.selectByPrimaryKey(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在: " + chapterId);
        }

        generateContentForChapter(chapter);
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
            articleChapterMapper.updateByPrimaryKey(chapter);

            // 检查并更新文章完成状态
            checkAndUpdateArticleCompletionStatus(article);

            log.info("文章[{}]第{}章内容生成完成，字数：{}",
                    article.getArticleName(), chapter.getChapterNo(),
                    chapterContent != null ? chapterContent.length() : 0);

        } catch (Exception e) {
            log.error("生成文章[{}]第{}章内容失败：{}", article.getArticleName(), chapter.getChapterNo(), e.getMessage(), e);

            // 更新章节状态为生成失败
            chapter.setGenerationStatus(3); // 生成失败
            chapter.setUpdateTime(LocalDateTime.now());
            articleChapterMapper.updateByPrimaryKey(chapter);

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
            // 使用Responses API生成章节内容
            log.info("开始使用Responses API生成章节内容...");
            String generatedContent = responseIdManager.callAIWithResponsesAPI(article, prompt, "content");

            log.info("=== AI原始响应 ===");
            log.info("生成内容长度: {} 字符", generatedContent.length());

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
     * 检查并更新文章完成状态
     */
    private void checkAndUpdateArticleCompletionStatus(Article article) {
        try {
            // 查询文章的所有章节
            List<ArticleChapter> allChapters = articleChapterMapper.selectByArticleId(article.getId());
            if (allChapters == null || allChapters.isEmpty()) {
                return;
            }

            boolean allCompleted = true;
            for (ArticleChapter chapter : allChapters) {
                // 如果章节没有内容且生成状态不是失败，则认为未完成
                if (!StringUtils.hasText(chapter.getChapterContent()) &&
                    (chapter.getGenerationStatus() == null || chapter.getGenerationStatus() != 3)) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) {
                // 更新文章状态为已完成
                article.setGenerationStatus(2); // 已完成
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateByPrimaryKey(article);
                log.info("文章[{}]所有章节内容生成完成，更新文章状态为已完成", article.getArticleName());
            }
        } catch (Exception e) {
            log.warn("检查文章完成状态失败：{}", e.getMessage());
        }
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

        // 文章背景信息
        if (StringUtils.hasText(article.getArticleOutline())) {
            prompt.append("小说大纲：\n").append(article.getArticleOutline()).append("\n\n");
        }
        if (StringUtils.hasText(article.getStoryBackground())) {
            prompt.append("故事背景：\n").append(article.getStoryBackground()).append("\n\n");
        }

        // 章节基本信息
        prompt.append("第").append(chapter.getChapterNo()).append("章标题：").append(chapter.getChapterTitle()).append("\n");
        if (StringUtils.hasText(chapter.getCorePlot())) {
            prompt.append("核心情节：").append(chapter.getCorePlot()).append("\n");
        }
        prompt.append("预估字数：").append(chapter.getWordCountEstimate() != null ? chapter.getWordCountEstimate() : 2000).append("字\n\n");

        // 生成配置信息
        if (config != null) {
            if (StringUtils.hasText(config.getAdditionalCharacteristics())) {
                prompt.append("附加特征：").append(config.getAdditionalCharacteristics()).append("\n");
            }
        }

        // 伏笔信息
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

        // 生成要求
        prompt.append("请根据以上信息，创作一篇完整的小说章节内容。要求：\n");
        prompt.append("1. 内容要符合章节标题和核心情节\n");
        prompt.append("2. 文字流畅，情节连贯，符合小说风格\n");
        prompt.append("3. 合理运用本章设置的伏笔，为后续情节做铺垫\n");
        prompt.append("4. 适时回收相关的伏笔，推动情节发展\n");
        prompt.append("5. 字数控制在预估字数左右（")
                .append(chapter.getWordCountEstimate() != null ? chapter.getWordCountEstimate() : 2000)
                .append("字）\n");
        prompt.append("6. 直接输出章节正文内容，不要包含章节标题\n");

        return prompt.toString();
    }
}