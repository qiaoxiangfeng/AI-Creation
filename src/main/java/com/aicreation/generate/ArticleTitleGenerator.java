package com.aicreation.generate;

import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleGenerationConfig;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.mapper.ArticleGenerationConfigMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.service.IArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * 生成单个文章标题
     *
     * @param configId 文章生成配置ID
     * @return 生成的文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long generateSingleTitle(Long configId) {
        log.info("开始为配置[{}]生成单个文章标题", configId);

        try {
            // 获取配置信息
            ArticleGenerationConfig config = articleGenerationConfigMapper.selectByPrimaryKey(configId);
            if (config == null) {
                throw new RuntimeException("文章生成配置不存在: " + configId);
            }

            // 查询该文章主题下已存在的文章标题
            List<String> existingTitles = getExistingTitlesByArticleType(config.getTheme());

            // 构建生成文章标题和大纲的提示词
            String prompt = buildTitleGenerationPrompt(config, existingTitles);

            log.debug("=== AI标题生成提示词 ===");
            log.debug("{}", prompt);

            // 使用Responses API生成内容（首次调用，previous_response_id为null）
            // 非流式调用：API会在生成全部内容后一次性返回完整响应，收到返回即代表输出完成
            log.info("开始使用非流式Responses API生成文章标题和大纲...");
            com.volcengine.ark.runtime.model.responses.response.ResponseObject response =
                volcengineChatClient.createResponse(
                    prompt,
                    null, // 首次调用，previous_response_id为null
                    "title" // 标题生成任务
                );

            // 获取生成的內容
            String generatedContent = com.aicreation.external.ResponseContentExtractor.extractContent(response);

            // 获取返回的response_id
            String responseId = response.getId();

            log.info("非流式响应完成，获取内容长度: {}", generatedContent.length());

            log.info("=== AI原始响应 ===");
            log.info("AI响应内容长度: {}", generatedContent != null ? generatedContent.length() : 0);
            if (generatedContent != null && !generatedContent.isEmpty()) {
                log.info("AI响应内容(前200字符): {}", generatedContent.substring(0, Math.min(200, generatedContent.length())));
            } else {
                log.warn("AI响应内容为空！");
            }
            log.info("获得的response_id: {}", responseId);

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
            Long articleId = articleService.createArticle(createReq);

            // 设置从标题生成API获得的response_id
            if (responseId != null && !responseId.trim().isEmpty()) {
                articleMapper.updateResponseId(articleId, responseId);
                log.info("文章[{}]的response_id已设置为: {}", articleId, responseId);
            } else {
                log.warn("标题生成API未返回有效的response_id，文章后续AI交互可能无法保持上下文");
            }

            log.info("成功为配置[{}]生成单个文章标题：标题={}, ID={}, response_id={}",
                    configId, content.getTitle(), articleId, responseId);

            return articleId;

        } catch (Exception e) {
            log.error("为配置[{}]生成单个文章标题失败: {}", configId, e.getMessage(), e);
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
            return articleMapper.selectArticleNamesByType(articleType);
        }
        return new ArrayList<>();
    }

    /**
     * 构建标题生成提示词
     */
    private String buildTitleGenerationPrompt(ArticleGenerationConfig config, List<String> existingTitles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下配置生成一个新的小说标题和大纲：\n\n");

        // 配置信息
        prompt.append("小说类型：").append(config.getTheme()).append("\n");
        if (StringUtils.hasText(config.getAdditionalCharacteristics())) {
            prompt.append("附加特征：").append(config.getAdditionalCharacteristics()).append("\n");
        }

        // 现有标题（用于去重）
        if (!existingTitles.isEmpty()) {
            prompt.append("\n已有标题（请避免重复）：\n");
            for (int i = 0; i < Math.min(existingTitles.size(), 10); i++) {
                prompt.append("- ").append(existingTitles.get(i)).append("\n");
            }
        }

        prompt.append("\n请生成一个吸引人的小说标题和详细大纲：\n");
        prompt.append("1. title：小说标题（创意新颖，吸引读者）\n");
        prompt.append("2. outline：小说大纲（300-500字，包含主要情节框架、世界观设定、核心冲突）\n");
        prompt.append("3. storyBackground：故事背景（100-200字，世界观和人物背景介绍）\n");

        prompt.append("\n生成要求：\n");
        prompt.append("请根据以上要求直接输出JSON格式，不要任何解释或思考过程。\n\n");

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
        req.setArticleType(config.getTheme());
        req.setPublishStatus(0); // 未发布
        req.setGenerationStatus(0); // 未开始生成
        return req;
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