package com.aicreation.generate;

import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.dto.ArticleTitleDedupItemDto;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.security.AccessControlService;
import com.aicreation.service.AiBillingService;
import com.aicreation.service.IArticleService;
import com.aicreation.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章标题生成器
 * 提供单次文章标题生成的接口
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
@Service
public class ArticleTitleGenerator {

    @Autowired
    private ArticleGenerationConfigMapper articleGenerationConfigMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private IArticleService articleService;

    @Autowired
    private VolcengineChatClient volcengineChatClient;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AiBillingService aiBillingService;

    /**
     * 生成单个文章标题
     * 无事务保护，数据实时提交
     *
     * @param configId 文章生成配置ID
     * @return 生成的文章ID
     */
    public Long generateSingleTitle(Long configId) {
        log.info("开始为配置[{}]生成单个文章标题", configId);

        try {
            accessControlService.assertGenerationConfigAccess(configId);

            // 获取配置信息
            ArticleGenerationConfig config = articleGenerationConfigMapper.selectByPrimaryKey(configId);
            if (config == null) {
                throw new RuntimeException("文章生成配置不存在: " + configId);
            }

            // 查询该主题下已生成的文章（标题+大纲），用于去重
            List<ArticleTitleDedupItemDto> existingItems = getExistingTitleDedupItemsByTheme(config.getTheme());

            // 构建生成文章标题和大纲的提示词
            String prompt = buildTitleGenerationPrompt(config, existingItems);

            log.info("=== AI标题生成完整提示词 ===");
            log.info("{}", prompt);

            // 使用Responses API生成内容（首次调用，previous_response_id为null）
            // 非流式调用：API会在生成全部内容后一次性返回完整响应，收到返回即代表输出完成
            log.info("开始使用非流式Responses API生成文章标题和大纲...");
            log.info("本次AI标题生成请求 previous_response_id: null");
            Long userId = config.getCreateUserId();
            long estimatedCostCent = aiBillingService.estimateCostCent(
                "GENERATE_SINGLE_TITLE",
                config.getChapterWordCountEstimate() != null ? config.getChapterWordCountEstimate() : config.getTotalWordCountEstimate()
            );

            com.volcengine.ark.runtime.model.responses.response.ResponseObject response =
                aiBillingService.executeWithAiBilling(
                    userId,
                    "GENERATE_SINGLE_TITLE",
                    null,
                    null,
                    estimatedCostCent,
                    () -> volcengineChatClient.createResponse(
                        prompt,
                        null, // 首次调用，previous_response_id为null
                        "title" // 标题生成任务
                    )
                );

            // 获取生成的內容
            String generatedContent = com.aicreation.external.ResponseContentExtractor.extractContent(response);

            log.info("非流式响应完成，获取内容长度: {}", generatedContent.length());

            log.info("=== AI原始响应 ===");
            log.info("AI响应内容长度: {}", generatedContent != null ? generatedContent.length() : 0);
            if (generatedContent != null && !generatedContent.isEmpty()) {
                log.info("AI响应内容(前200字符): {}", generatedContent.substring(0, Math.min(200, generatedContent.length())));
            } else {
                log.warn("AI响应内容为空！");
            }
            // 检查JSON是否完整
            if (!isJsonComplete(generatedContent)) {
                log.error("AI返回的JSON格式不完整，内容长度: {}, 最后100字符: {}",
                    generatedContent.length(),
                    generatedContent.length() > 100 ?
                        generatedContent.substring(generatedContent.length() - 100) :
                        generatedContent);
                throw new RuntimeException("AI返回的JSON格式不完整，响应可能被截断");
            }

            // 解析生成的內容
            GeneratedContent content = parseGeneratedContent(generatedContent);

            // 创建文章（只包含标题和大纲）
            ArticleCreateReqDto createReq = buildArticleCreateReq(config, content);
            // 标题生成完成后不再存储 response_id（避免无意义的上下文绑定）
            Long articleId = articleService.createArticle(createReq);

            log.info("成功为配置[{}]生成单个文章标题：标题={}, ID={}",
                    configId, content.getTitle(), articleId);

            return articleId;

        } catch (Exception e) {
            log.error("为配置[{}]生成单个文章标题失败: {}", configId, e.getMessage(), e);
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new RuntimeException("生成文章标题失败: " + e.getMessage(), e);
        }
    }


    /**
     * 检查JSON字符串是否完整
     */
    private boolean isJsonComplete(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }

        jsonStr = jsonStr.trim();

        // 检查是否以 { 开头，以 } 结尾
        if (!jsonStr.startsWith("{") || !jsonStr.endsWith("}")) {
            log.debug("JSON格式检查失败：不是以{{}开头或}}结尾");
            return false;
        }

        // 简单的括号匹配检查
        int openBraces = 0;
        int closeBraces = 0;
        boolean inString = false;
        char prevChar = '\0';

        for (char c : jsonStr.toCharArray()) {
            if (inString) {
                // 在字符串中，只检查转义字符
                if (c == '"' && prevChar != '\\') {
                    inString = false;
                }
            } else {
                // 不在字符串中
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    openBraces++;
                } else if (c == '}') {
                    closeBraces++;
                }
            }
            prevChar = c;
        }

        // 检查括号是否匹配
        if (openBraces != closeBraces) {
            log.debug("JSON格式检查失败：括号不匹配，开括号:{}, 闭括号:{}", openBraces, closeBraces);
            return false;
        }

        log.debug("JSON格式检查通过");
        return true;
    }

    /**
     * 获取指定文章类型的所有现有标题
     */
    private List<String> getExistingTitlesByArticleType(String articleType) {
        if (StringUtils.hasText(articleType)) {
            return articleMapper.selectArticleNamesByTheme(articleType);
        }
        return new ArrayList<>();
    }

    private List<ArticleTitleDedupItemDto> getExistingTitleDedupItemsByTheme(String theme) {
        if (StringUtils.hasText(theme)) {
            return articleMapper.selectExistingTitleDedupItemsByTheme(theme);
        }
        return new ArrayList<>();
    }

    /**
     * 构建标题生成提示词
     */
    private String buildTitleGenerationPrompt(ArticleGenerationConfig config, List<ArticleTitleDedupItemDto> existingItems) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名擅长中文网络小说与大众出版市场的专业编辑，请根据下面的“小说生成配置”生成一个新的小说标题、大纲和故事背景。\n\n");

        // 配置信息（完整使用文章生成配置的各个字段）
        prompt.append("【小说生成配置】\n");
        prompt.append("- 主题（Theme）：").append(config.getTheme()).append("\n");
        if (StringUtils.hasText(config.getGender())) {
            prompt.append("- 性别向（Gender）：").append(config.getGender()).append("\n");
        }
        if (StringUtils.hasText(config.getGenre())) {
            prompt.append("- 题材（Genre）：").append(config.getGenre()).append("\n");
        }
        if (StringUtils.hasText(config.getPlot())) {
            prompt.append("- 情节类型（Plot）：").append(config.getPlot()).append("\n");
        }
        if (StringUtils.hasText(config.getCharacterType())) {
            prompt.append("- 角色类型（CharacterType）：").append(config.getCharacterType()).append("\n");
        }
        if (StringUtils.hasText(config.getStyle())) {
            prompt.append("- 整体风格（Style）：").append(config.getStyle()).append("\n");
        }
        if (StringUtils.hasText(config.getAdditionalCharacteristics())) {
            prompt.append("- 附加特点（AdditionalCharacteristics）：")
                  .append(config.getAdditionalCharacteristics()).append("\n");
        }
        if (config.getTotalWordCountEstimate() != null) {
            prompt.append("- 总字数预估（TotalWordCountEstimate）：")
                  .append(config.getTotalWordCountEstimate()).append(" 字左右\n");
        }
        if (config.getChapterWordCountEstimate() != null) {
            prompt.append("- 每章字数预估（ChapterWordCountEstimate）：")
                  .append(config.getChapterWordCountEstimate()).append(" 字左右\n");
        }

        // 已生成文章（标题 + 大纲，用于去重）
        if (existingItems != null && !existingItems.isEmpty()) {
            prompt.append("\n已生成文章列表（新标题与新大纲必须与下列任意一个在标题或大纲上都不能相同或仅作轻微改动）：\n");
            for (int i = 0; i < Math.min(existingItems.size(), 10); i++) {
                ArticleTitleDedupItemDto it = existingItems.get(i);
                if (it == null) continue;
                String name = it.getArticleName() != null ? it.getArticleName().trim() : "";
                String outline = it.getArticleOutline() != null ? it.getArticleOutline().trim() : "";
                if (!name.isEmpty() && !outline.isEmpty()) {
                    prompt.append("- 标题：").append(name).append("\n");
                    prompt.append("  大纲：").append(outline).append("\n");
                } else if (!name.isEmpty()) {
                    prompt.append("- 标题：").append(name).append("\n");
                }
            }
        }

        // 根据总字数预估动态调整大纲字数区间
        Integer totalWordCount = config.getTotalWordCountEstimate();
        String outlineRange;
        if (totalWordCount == null) {
            outlineRange = "300-500字";
        } else if (totalWordCount <= 30000) {
            outlineRange = "150-200字";
        } else if (totalWordCount <= 100000) {
            outlineRange = "300-500字";
        } else if (totalWordCount <= 500000) {
            outlineRange = "500-700字";
        } else if (totalWordCount <= 1000000) {
            outlineRange = "700-1000字";
        } else {
            outlineRange = "1000-2000字";
        }

        prompt.append("\n请在充分理解上述配置的基础上，生成一个全新的小说标题及配套大纲、故事背景：\n");
        prompt.append("1. title：小说标题。\n");
        prompt.append("   - 语言：中文。\n");
        prompt.append("   - 长度：一般控制在10～20个汉字之间，避免过长或过短。\n");
        prompt.append("   - 风格：需明显符合【主题 / 性别向 / 题材 / 情节类型 / 角色类型 / 整体风格】的受众预期，具有清晰的“卖点”（如强冲突、强爽点、强情绪或强悬念）。\n");
        prompt.append("   - 不能与“已有标题列表”中的任何一个标题相同，也不能只做一两个字的小改动（如仅替换同义词或简单加减一两个字）。\n");
        prompt.append("   - 尽量避免使用生僻词和难以理解的隐喻，保证一眼就能看出题材与爽点。\n");
        prompt.append("   - 可以使用书名号《》或不使用，二者选其一，不要混用其它花哨符号。\n");
        prompt.append("2. outline：小说大纲（").append(outlineRange).append("）。\n");
        prompt.append("   - 需要涵盖：主线冲突、男女主或核心角色设定、世界观框架、故事走向（开局—发展—高潮—结局的大致安排）。\n");
        prompt.append("   - 要明显服务于上面的标题设计，让读者从大纲中看到标题所承诺的爽点或矛盾如何展开。\n");
        prompt.append("3. storyBackground：故事背景（100-200字）。\n");
        prompt.append("   - 更侧重世界观设定、时代背景、社会环境或关键规则设定，为后续章节创作提供稳定的基底。\n");

        prompt.append("\n生成要求：\n");
        prompt.append("- 必须严格按照下方给出的JSON字段名输出，不要新增、删除或重命名字段。\n");
        prompt.append("- 不要输出任何分析过程、解释文字、标注、Markdown语法或多余内容，只输出一个完整的JSON对象。\n");
        prompt.append("- 字符串内容中如需使用双引号，请进行正确的JSON转义。\n\n");

        prompt.append("输出格式（只输出JSON，不要其他内容）：\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"小说标题\",\n");
        prompt.append("  \"outline\": \"小说大纲内容\",\n");
        prompt.append("  \"storyBackground\": \"故事背景内容\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 解析生成的內容
     */
    private GeneratedContent parseGeneratedContent(String content) {
        try {
            // 首先尝试直接解析JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> jsonMap = objectMapper.readValue(content, java.util.Map.class);

            GeneratedContent result = new GeneratedContent();
            result.setTitle((String) jsonMap.get("title"));
            result.setOutline((String) jsonMap.get("outline"));
            result.setStoryBackground((String) jsonMap.get("storyBackground"));

            log.info("标题生成解析完成 - 标题: {}, 大纲长度: {}, 背景长度: {}",
                     result.getTitle(),
                     result.getOutline() != null ? result.getOutline().length() : 0,
                     result.getStoryBackground() != null ? result.getStoryBackground().length() : 0);

            return result;

        } catch (Exception e) {
            log.warn("直接解析JSON失败，尝试提取JSON内容: {}", e.getMessage());

            // 如果直接解析失败，尝试从内容中提取JSON部分
            try {
                String jsonContent = extractJsonFromContent(content);
                if (jsonContent != null) {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, java.util.Map.class);

                    GeneratedContent result = new GeneratedContent();
                    result.setTitle((String) jsonMap.get("title"));
                    result.setOutline((String) jsonMap.get("outline"));
                    result.setStoryBackground((String) jsonMap.get("storyBackground"));

                    log.info("从推理内容中成功提取并解析JSON - 标题: {}, 大纲长度: {}, 背景长度: {}",
                             result.getTitle(),
                             result.getOutline() != null ? result.getOutline().length() : 0,
                             result.getStoryBackground() != null ? result.getStoryBackground().length() : 0);

                    return result;
                }
            } catch (Exception extractException) {
                log.error("提取JSON内容也失败: {}", extractException.getMessage());
            }

            log.error("解析标题生成内容失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI返回格式错误，无法解析标题内容: " + e.getMessage(), e);
        }
    }

    /**
     * 从AI返回的内容中提取JSON部分
     */
    private String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            // 查找第一个 '{' 和最后一个 '}'
            int startIndex = content.indexOf('{');
            int endIndex = content.lastIndexOf('}');

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String jsonCandidate = content.substring(startIndex, endIndex + 1);

                // 验证是否是有效的JSON
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                objectMapper.readTree(jsonCandidate); // 只是验证，不需要结果

                log.info("成功提取JSON内容，长度: {}", jsonCandidate.length());
                return jsonCandidate;
            }
        } catch (Exception e) {
            log.warn("提取的JSON内容无效: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 构建文章创建请求
     */
    private ArticleCreateReqDto buildArticleCreateReq(ArticleGenerationConfig config, GeneratedContent content) {
        ArticleCreateReqDto req = new ArticleCreateReqDto();
        req.setArticleName(content.getTitle());
        req.setArticleOutline(content.getOutline());
        req.setStoryBackground(content.getStoryBackground());
        req.setTheme(config.getTheme());
        // 将“除主题外”的配置字段值拼接到文章表附加特点（逗号分隔），用于后续章节生成提示词参考
        req.setAdditionalCharacteristics(buildAdditionalCharacteristicsForArticle(config));
        // 从文章生成配置中继承总字数预估和每章节字数预估，若为空则使用默认值
        Integer totalEstimate = config.getTotalWordCountEstimate() != null ? config.getTotalWordCountEstimate() : 100000;
        Integer chapterEstimate = config.getChapterWordCountEstimate() != null ? config.getChapterWordCountEstimate() : 5000;
        req.setTotalWordCountEstimate(totalEstimate);
        req.setChapterWordCountEstimate(chapterEstimate);
        req.setPublishStatus(0); // 未发布
        // 与文章生成配置的创建人一致
        req.setCreateUserId(config.getCreateUserId());
        return req;
    }

    /**
     * 将文章生成配置中除主题外的字段值拼接为逗号分隔字符串
     */
    private String buildAdditionalCharacteristicsForArticle(ArticleGenerationConfig config) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(config.getGender())) parts.add(config.getGender().trim());
        if (StringUtils.hasText(config.getGenre())) parts.add(config.getGenre().trim());
        if (StringUtils.hasText(config.getPlot())) parts.add(config.getPlot().trim());
        if (StringUtils.hasText(config.getCharacterType())) parts.add(config.getCharacterType().trim());
        if (StringUtils.hasText(config.getStyle())) parts.add(config.getStyle().trim());
        if (StringUtils.hasText(config.getAdditionalCharacteristics())) parts.add(config.getAdditionalCharacteristics().trim());
        return String.join(",", parts);
    }

    /**
     * 生成的内容内部类
     */
    public static class GeneratedContent {
        private String title;
        private String outline;
        private String storyBackground;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getOutline() { return outline; }
        public void setOutline(String outline) { this.outline = outline; }

        public String getStoryBackground() { return storyBackground; }
        public void setStoryBackground(String storyBackground) { this.storyBackground = storyBackground; }
    }
}