package com.aicreation.task;

import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.dto.ArticleGenerationConfigListReqDto;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.enums.ArticleStatusEnum;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.external.DouBaoClient;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.util.TraceUtil;
import com.aicreation.service.IArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文章标题生成定时任务
 * 根据文章主题和待生成数量自动生成文章标题和大纲
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Slf4j
@Component
public class ArticleTitleGenerationTask {

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Autowired
    private com.aicreation.mapper.ArticleMapper articleMapper;

    @Autowired
    private IArticleService articleService;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    /**
     * 任务执行状态标记，避免并发执行
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 定时任务：每月1号执行文章标题生成
     * 如果上次执行未完成，则丢弃本次调度
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 每月1号0点0分0秒执行
    @Transactional(rollbackFor = Exception.class)
    public void generateArticleTitles() {
        TraceUtil.executeWithTraceId(() -> {
            executeArticleTitlesTask();
        });
    }

    /**
     * 执行文章标题生成任务的具体逻辑
     */
    private void executeArticleTitlesTask() {
        // 检查任务是否正在执行，如果是则跳过本次执行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("文章标题生成定时任务正在执行中，跳过本次调度");
            return;
        }

        log.info("开始执行文章标题生成定时任务");

        try {
            // 查询所有待生成数量大于0的文章分类
            List<ArticleGenerationConfig> articleTypes = articleGenerationConfigMapper.selectPendingArticleGenerationConfigs();

            for (ArticleGenerationConfig articleType : articleTypes) {
                if (articleType.getPendingCount() != null && articleType.getPendingCount() > 0) {
                    generateTitlesForType(articleType);
                }
            }

            log.info("文章标题生成定时任务执行完成");
        } catch (Exception e) {
            log.error("文章标题生成定时任务执行失败", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        } finally {
            // 重置执行状态
            isRunning.set(false);
        }
    }

    /**
     * 为指定文章类型生成文章标题和大纲
     *
     * @param articleType 文章类型
     */
    private void generateTitlesForType(ArticleGenerationConfig articleType) {
        log.info("开始为文章主题[{}]生成{}个标题", articleType.getTheme(), articleType.getPendingCount());

        int successCount = 0;
        int failCount = 0;
        boolean hasTransactionError = false;

        for (int i = 0; i < articleType.getPendingCount(); i++) {
            try {
                // 如果之前有事务错误，停止处理
                if (hasTransactionError) {
                    log.warn("由于之前的数据库事务错误，停止处理剩余的文章生成任务");
                    break;
                }

                // 生成文章标题和大纲
                GeneratedContent content = generateArticleContent(articleType);

                // 创建文章（只包含标题和大纲）
                ArticleCreateReqDto createReq = buildArticleCreateReq(articleType, content);
                Long articleId = articleService.createArticle(createReq);

                log.info("成功生成文章标题：主题={}, 标题={}, ID={}",
                        articleType.getTheme(), content.getTitle(), articleId);

                successCount++;

                // 添加短暂延迟，避免API调用过于频繁
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("生成文章标题失败：主题={}, 错误={}", articleType.getTheme(), e.getMessage(), e);

                // 检查是否是数据库事务相关错误
                if (e.getMessage() != null &&
                    (e.getMessage().contains("current transaction is aborted") ||
                     e.getMessage().contains("duplicate key value"))) {
                    hasTransactionError = true;
                    log.error("检测到数据库事务错误，将停止当前批次的处理");
                }

                failCount++;
            }
        }

        // 只有在没有事务错误的情况下才更新待生成数量
        if (!hasTransactionError) {
            try {
                int remainingCount = Math.max(0, articleType.getPendingCount() - successCount);
                updatePendingCount(articleType.getId(), remainingCount);
                log.info("文章主题[{}]标题生成完成：成功{}个，失败{}个，剩余待生成{}个",
                        articleType.getTheme(), successCount, failCount, remainingCount);
            } catch (Exception e) {
                log.error("更新待生成数量失败：articleTypeId={}, newCount={}, 错误={}",
                        articleType.getId(), articleType.getPendingCount() - successCount, e.getMessage(), e);
                // 不抛出异常，避免影响其他任务
            }
        } else {
            log.warn("由于数据库事务错误，跳过待生成数量的更新，建议手动检查数据一致性");
        }
    }

    /**
     * 查询指定文章类型下已存在的文章标题
     *
     * @param articleType 文章类型
     * @return 已存在的文章标题列表
     */
    private List<String> getExistingTitlesByArticleType(String articleType) {
        try {
            return articleMapper.selectExistingTitlesByArticleType(articleType);
        } catch (Exception e) {
            log.warn("查询已存在文章标题失败：articleType={}, error={}", articleType, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 使用AI生成文章内容
     *
     * @param articleType 文章类型
     * @param articleCharacteristics 文章特点
     * @return 生成的内容
     */
    private GeneratedContent generateArticleContent(ArticleGenerationConfig articleType) {
        // 查询该文章主题下已存在的文章标题
        List<String> existingTitles = getExistingTitlesByArticleType(articleType.getTheme());

        // 构建生成文章标题和大纲的提示词
        String prompt = buildTitleGenerationPrompt(articleType, existingTitles);

        log.debug("=== AI标题生成提示词 ===");
        log.debug("{}", prompt);

        try {
            // 使用流式响应调用豆包AI生成内容
            log.info("开始流式调用AI生成文章标题和大纲...");
            String generatedContent = callAIWithStreaming(
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "doubao-seed-1-6-lite-251015",
                prompt
            );

            return parseGeneratedContent(generatedContent);

        } catch (Exception e) {
            log.error("调用AI生成文章内容失败：{}", e.getMessage());
            // 返回默认内容作为降级方案
            return getFallbackContent(articleType.getTheme());
        }
    }

    /**
     * 使用流式响应调用AI并返回完整内容
     */
    private String callAIWithStreaming(String url, String model, String prompt) {
        StringBuilder fullContent = new StringBuilder();

        try {
            volcengineChatClient.streamChatCompletion(url, model, prompt, chunk -> {
                if (chunk != null && !chunk.startsWith("[ERROR]")) {
                    fullContent.append(chunk);
                } else if (chunk != null && chunk.startsWith("[ERROR]")) {
                    log.error("流式响应错误: {}", chunk);
                }
            });

            log.info("流式响应完成，总内容长度: {}", fullContent.length());
            return fullContent.toString();

        } catch (Exception e) {
            log.error("流式调用AI失败", e);
            throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建文章标题生成提示词
     *
     * @param config 文章生成配置
     * @param existingTitles 已存在的文章标题列表
     * @return 提示词
     */
    private String buildTitleGenerationPrompt(ArticleGenerationConfig config, List<String> existingTitles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("请为主题「%s」的文章生成一个吸引人的标题和详细的大纲。\n", config.getTheme()));

        // 添加详细的分类要求
        List<String> requirements = new ArrayList<>();

        if (StringUtils.hasText(config.getGender())) {
            requirements.add(String.format("性别定位：%s", config.getGender()));
        }
        if (StringUtils.hasText(config.getGenre())) {
            requirements.add(String.format("题材类型：%s", config.getGenre()));
        }
        if (StringUtils.hasText(config.getPlot())) {
            requirements.add(String.format("情节类型：%s", config.getPlot()));
        }
        if (StringUtils.hasText(config.getCharacterType())) {
            requirements.add(String.format("角色类型：%s", config.getCharacterType()));
        }
        if (StringUtils.hasText(config.getStyle())) {
            requirements.add(String.format("风格类型：%s", config.getStyle()));
        }
        if (StringUtils.hasText(config.getAdditionalCharacteristics())) {
            requirements.add(String.format("附加特点：%s", config.getAdditionalCharacteristics()));
        }

        if (!requirements.isEmpty()) {
            prompt.append("文章要求：\n");
            for (int i = 0; i < requirements.size(); i++) {
                prompt.append(String.format("%d. %s\n", i + 1, requirements.get(i)));
            }
            prompt.append("请确保标题和大纲完全符合以上所有要求。\n");
        }

        prompt.append("要求：\n");
        prompt.append("1. 标题要简洁、有吸引力，不超过20个字\n");
        prompt.append("2. 大纲要详细、逻辑清晰，包含主要内容要点\n");
        prompt.append(String.format("3. 风格要符合「%s」主题的特点\n", config.getTheme()));
        prompt.append("4. 大纲长度控制在200-500字\n");

        // 添加排除已存在标题的指令
        int itemNumber = 5;
        if (existingTitles != null && !existingTitles.isEmpty()) {
            prompt.append(String.format("%d. 标题不能与以下已存在的标题重复：\n", itemNumber));
            for (int i = 0; i < existingTitles.size(); i++) {
                prompt.append(String.format("   %d. %s\n", i + 1, existingTitles.get(i)));
            }
            prompt.append("   请确保生成全新的标题，避免任何重复\n");
        }

        prompt.append("\n请按以下JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"articleName\": \"文章标题内容\",\n");
        prompt.append("  \"articleOutline\": \"大纲内容\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 解析AI生成的内容
     *
     * @param content AI生成的内容
     * @return 解析后的内容
     */
    private GeneratedContent parseGeneratedContent(String content) {
        GeneratedContent result = new GeneratedContent();

        try {
            // 尝试解析JSON格式
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(content);

            // 提取文章名称
            if (jsonNode.has("articleName") && jsonNode.get("articleName").isTextual()) {
                result.setTitle(jsonNode.get("articleName").asText());
            }

            // 提取文章大纲
            if (jsonNode.has("articleOutline") && jsonNode.get("articleOutline").isTextual()) {
                result.setOutline(jsonNode.get("articleOutline").asText());
            }

            // 如果JSON解析失败，尝试回退到旧的字符串解析
            if (!StringUtils.hasText(result.getTitle()) && !StringUtils.hasText(result.getOutline())) {
                // 解析标题
                int titleStart = content.indexOf("标题：[");
                int titleEnd = content.indexOf("]", titleStart);
                if (titleStart >= 0 && titleEnd > titleStart) {
                    result.setTitle(content.substring(titleStart + 4, titleEnd));
                }

                // 解析大纲
                int outlineStart = content.indexOf("大纲：[");
                int outlineEnd = content.indexOf("]", outlineStart);
                if (outlineStart >= 0 && outlineEnd > outlineStart) {
                    result.setOutline(content.substring(outlineStart + 4, outlineEnd));
                }
            }

            // 如果解析失败，使用默认内容
            if (!StringUtils.hasText(result.getTitle())) {
                result.setTitle("AI生成文章 - " + LocalDateTime.now().toString());
            }
            if (!StringUtils.hasText(result.getOutline())) {
                result.setOutline(content);
            }

        } catch (Exception e) {
            log.warn("解析AI生成内容失败，使用默认内容：{}", e.getMessage());
            result.setTitle("AI生成文章 - " + LocalDateTime.now().toString());
            result.setOutline(content);
        }

        return result;
    }

    /**
     * 获取降级内容（当AI调用失败时使用）
     *
     * @param articleType 文章类型
     * @return 默认内容
     */
    private GeneratedContent getFallbackContent(String articleType) {
        GeneratedContent content = new GeneratedContent();
        content.setTitle(articleType + " - " + LocalDateTime.now().toString());
        content.setOutline(String.format(
            "%s相关内容介绍：\n\n" +
            "1. 背景介绍\n" +
            "2. 主要特点\n" +
            "3. 发展趋势\n" +
            "4. 应用场景\n" +
            "5. 总结与展望",
            articleType
        ));
        return content;
    }

    /**
     * 构建文章创建请求
     *
     * @param articleType 文章类型
     * @param content 生成的内容
     * @return 创建请求
     */
    private ArticleCreateReqDto buildArticleCreateReq(ArticleGenerationConfig articleType, GeneratedContent content) {
        ArticleCreateReqDto req = new ArticleCreateReqDto();
        req.setArticleName(content.getTitle());
        req.setArticleOutline(content.getOutline());

        // 构建文章类型描述，包含主题和所有分类信息
        StringBuilder articleTypeDesc = new StringBuilder();
        articleTypeDesc.append(articleType.getTheme());

        List<String> classifications = new ArrayList<>();
        if (StringUtils.hasText(articleType.getGender())) {
            classifications.add(articleType.getGender());
        }
        if (StringUtils.hasText(articleType.getGenre())) {
            classifications.add(articleType.getGenre());
        }
        if (StringUtils.hasText(articleType.getPlot())) {
            classifications.add(articleType.getPlot());
        }
        if (StringUtils.hasText(articleType.getCharacterType())) {
            classifications.add(articleType.getCharacterType());
        }
        if (StringUtils.hasText(articleType.getStyle())) {
            classifications.add(articleType.getStyle());
        }

        if (!classifications.isEmpty()) {
            articleTypeDesc.append("（").append(String.join("、", classifications)).append("）");
        }

        req.setArticleType(articleTypeDesc.toString());

        // 设置文章特点
        if (StringUtils.hasText(articleType.getAdditionalCharacteristics())) {
            req.setArticleCharacteristics(articleType.getAdditionalCharacteristics());
        }

        req.setPublishStatus(ArticleStatusEnum.UNPUBLISHED.getCode()); // 默认未发布
        return req;
    }

    /**
     * 更新待生成数量
     *
     * @param articleTypeId 文章类型ID
     * @param newCount 新的待生成数量
     */
    private void updatePendingCount(Long articleTypeId, int newCount) {
        try {
            ArticleGenerationConfig updateType = new ArticleGenerationConfig();
            updateType.setId(articleTypeId);
            updateType.setPendingCount(newCount);
            updateType.setUpdateTime(LocalDateTime.now());

            articleGenerationConfigMapper.updateByPrimaryKey(updateType);
            log.info("更新文章分类ID={}的待生成数量为{}", articleTypeId, newCount);
        } catch (Exception e) {
            log.error("更新待生成数量失败：articleTypeId={}, newCount={}", articleTypeId, newCount, e);
        }
    }

    /**
     * 生成的内容内部类
     */
    private static class GeneratedContent {
        private String title;
        private String outline;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOutline() {
            return outline;
        }

        public void setOutline(String outline) {
            this.outline = outline;
        }
    }
}