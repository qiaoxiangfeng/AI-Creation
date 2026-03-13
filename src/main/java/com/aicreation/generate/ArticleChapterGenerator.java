package com.aicreation.generate;

import com.aicreation.entity.po.Article;
import com.aicreation.entity.po.ArticleChapter;
import com.aicreation.entity.po.Plot;
import com.aicreation.mapper.ArticleChapterMapper;
import com.aicreation.mapper.ArticleMapper;
import com.aicreation.mapper.PlotMapper;
import com.aicreation.external.VolcengineChatClient;
import com.aicreation.external.ResponseContentExtractor;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 为指定文章生成章节
     * 注意：此方法必须同步执行，因为每个章节的生成都依赖于前一个章节生成的最新response_id
     * 来维护AI对话的上下文连续性
     * 数据实时落表，无事务保护，确保服务重启时数据不丢失
     *
     * @param articleId 文章ID
     */
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

        // 当前对话上下文所使用的 response_id，用于串联章节规划的上下文
        String currentResponseId = article.getResponseId();

        // 设置安全上限，防止无限循环（最大1000章）
        final int MAX_CHAPTERS = 1000;

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

                // 生成单个章节的基本信息（包含完结判断），并基于当前response_id串联上下文
                ChapterWithCompleteInfo chapterInfo =
                    generateSingleChapterWithCompleteCheck(article, currentChapterNo, currentResponseId);

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
                chapter.setResponseIdPlan(chapterInfo.getResponseIdPlan());
                chapter.setResState(1);
                chapter.setCreateTime(LocalDateTime.now());
                chapter.setUpdateTime(LocalDateTime.now());

                articleChapterMapper.insert(chapter);
                generatedCount++;

                // 验证章节ID是否正确设置
                if (chapter.getId() == null) {
                    log.error("章节插入后ID未正确设置，文章[{}]第{}章", article.getArticleName(), currentChapterNo);
                    throw new RuntimeException("章节ID生成失败");
                }
                log.debug("文章[{}]第{}章插入成功，ID: {}", article.getArticleName(), currentChapterNo, chapter.getId());

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

                // 更新当前对话上下文使用的 response_id，供下一章继续串联
                if (chapterInfo.getResponseIdPlan() != null && !chapterInfo.getResponseIdPlan().trim().isEmpty()) {
                    currentResponseId = chapterInfo.getResponseIdPlan();
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

        // 如果故事已完结，更新文章表的完结标识
        if (storyComplete) {
            try {
                Article articleToUpdate = articleMapper.selectByPrimaryKey(article.getId());
                if (articleToUpdate != null) {
                    articleToUpdate.setStoryComplete(true);
                    // 将最新的 response_id 写回文章，便于后续在最新上下文基础上继续生成
                    if (currentResponseId != null && !currentResponseId.trim().isEmpty()) {
                        articleToUpdate.setResponseId(currentResponseId);
                    }
                    articleToUpdate.setUpdateTime(LocalDateTime.now());
                    articleMapper.updateByPrimaryKey(articleToUpdate);
                    log.info("文章[{}]已标记为故事完结", article.getArticleName());
                }
            } catch (Exception e) {
                log.error("更新文章完结状态失败: {}", e.getMessage(), e);
                // 不抛出异常，因为主要任务已完成
            }
        }

        log.info("文章[{}]章节基本信息生成完成，共生成{}章，故事{}完结",
                article.getArticleName(), generatedCount, storyComplete ? "已" : "未");
    }

    /**
     * 生成单个章节的基本信息（包含完结判断）
     */
    public ChapterWithCompleteInfo generateSingleChapterWithCompleteCheck(Article article, int chapterNo, String previousResponseId) {
        String prompt = buildSingleChapterWithCompletePrompt(article, chapterNo);

        log.info("=== AI单章完结判断请求 ===");
        log.info("文章: {}", article.getArticleName());
        log.info("章节: 第{}章", chapterNo);
        log.info("AI提示词长度: {} 字符", prompt.length());
        log.debug("=== 完整AI提示词 ===");
        log.debug("{}", prompt);

        try {
            // 使用Responses API生成章节信息（基于传入的上下文response_id，获取本章专用的 response_id）
            log.info("开始使用Responses API生成单章信息...");
            log.info("本次AI请求 previous_response_id: {}", previousResponseId != null ? previousResponseId : "null");
            ResponseObject response = volcengineChatClient.createResponse(
                    prompt,
                    previousResponseId,
                    "content"
            );

            String generatedContent = ResponseContentExtractor.extractContent(response);

            log.info("=== AI原始响应 ===");
            log.info("AI返回的response_id: {}", response.getId());
            log.info("AI响应内容: {}", generatedContent);

            ChapterWithCompleteInfo parsedInfo = parseSingleChapterWithComplete(generatedContent, chapterNo);
            // 记录本章规划时使用的 response_id，供本章正文生成使用
            parsedInfo.setResponseIdPlan(response.getId());

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
        prompt.append("请在保持已有小说上下文连续性的前提下，生成第").append(chapterNo).append("章的基本信息：\n\n");

        // 计算预期章节数与有效每章字数，用于帮助AI判断是否该收尾
        Integer totalWordCountEstimate = article.getTotalWordCountEstimate();
        Integer chapterWordCountEstimate = article.getChapterWordCountEstimate();
        int effectiveChapterWordCount = (chapterWordCountEstimate != null && chapterWordCountEstimate > 0)
                ? chapterWordCountEstimate
                : 2000;

        int expectedChapters;
        if (totalWordCountEstimate != null && totalWordCountEstimate > 0 && effectiveChapterWordCount > 0) {
            double ratio = (double) totalWordCountEstimate / (double) effectiveChapterWordCount;
            expectedChapters = (int) Math.round(ratio);
            if (expectedChapters < 3) {
                expectedChapters = 3;
            }
        } else {
            // 当缺少总字数预估时，给一个保守的默认范围
            expectedChapters = 8;
        }

        // 第1章：给出完整的全局创作要求，后续章节只做精简提醒，避免重复大量提示词导致token膨胀
        if (chapterNo == 1) {
            // 全局创作要求：一次性说明本书的全局目标与章节生成规则，后续章节统一沿用
            prompt.append("【全局创作要求】（以下要求适用于本书全部章节，请从第1章开始严格执行，后续章节不得更改）：\n");

            // 文章基本信息（仅保留必要字段，避免重复提供已在上下文中的长文本）
            prompt.append("- 小说名称：").append(article.getArticleName()).append("\n");
            if (StringUtils.hasText(article.getArticleType())) {
                prompt.append("- 小说类型：").append(article.getArticleType()).append("\n");
            }

            // 全局创作目标，用于帮助AI控制整体篇幅与章节数量
            prompt.append("- 全文与章节篇幅规划（用于判断故事节奏和完结时机，而不是死板限制）：\n");
            if (totalWordCountEstimate != null && totalWordCountEstimate > 0) {
                prompt.append("  • 全文目标总字数：约").append(totalWordCountEstimate).append("字。\n");
            }
            prompt.append("  • 推荐的每章字数：约").append(effectiveChapterWordCount).append("字，可在合理范围内浮动。\n");
            prompt.append("  • 预期总章节数约为").append(expectedChapters)
                    .append("章，你可以在").append(expectedChapters - 1)
                    .append("～").append(expectedChapters + 1)
                    .append("章之间灵活调整，但不应无限制增加章节数。\n");

            // storyComplete 完结判断规则
            prompt.append("- 故事完结判断规则（适用于全部章节的 storyComplete 字段）：\n");
            prompt.append("  • 请结合当前章节号与预期总章节数").append(expectedChapters).append("进行判断。\n");
            prompt.append("  • 如果本章之后，核心矛盾和主要伏笔可以在接下来的0～1章内自然收束，")
                    .append("且整体篇幅已经接近或略高于预期总字数，应优先将 storyComplete 设置为 true，")
                    .append("并把收束核心矛盾、回收主要伏笔的设计放在本章或下一章中完成。\n");
            prompt.append("  • 如果当前或未来某一章节号已经大于").append(expectedChapters + 1)
                    .append("，除非还有非常关键且尚未展开或收束的主线，否则应将 storyComplete 设置为 true，避免无止境拆分新章节。\n");
            prompt.append("  • 只有在你确信故事还有重要主线没有展开或没有收束，且当前章节号明显低于预期章节数时，")
                    .append("才将 storyComplete 设置为 false 并继续拆分后续章节。\n");

            // 章节输出格式与字段结构统一要求（后续章节必须沿用）
            prompt.append("- 章节输出格式与字段结构统一要求：\n");
            prompt.append("  • 每一章必须只输出一个JSON对象，不能包含任何额外的解释文字或多个对象。\n");
            prompt.append("  • JSON对象中只能包含以下字段名，且字段名在全书范围内必须完全一致：\n");
            prompt.append("    - chapterTitle：字符串，本章标题；\n");
            prompt.append("    - corePlot：字符串，本章核心情节概述；\n");
            prompt.append("    - wordCountEstimate：数字，本章预估字数；\n");
            prompt.append("    - storyComplete：布尔值，表示故事是否在本章完结；\n");
            prompt.append("    - plots：数组（可选），每项包含 plotName / plotContent / recoveryChapter 三个字段。\n");
            prompt.append("  • 严禁新增、删除或重命名以上字段名，严禁更改JSON对象的整体结构。\n");
            prompt.append("  • 如果你对具体格式有疑问，请在后续章节中直接参考本章的JSON格式，完全复制字段名与结构，只修改对应章节的字段值。\n");

            // 伏笔与回收的全局要求（简要）
            prompt.append("- 伏笔设置与回收要求：\n");
            prompt.append("  • plots 中的每个伏笔应清晰描述名称、内容与预计回收章节（recoveryChapter），通常建议在2-4章内回收。\n");
            prompt.append("  • 当接近或超过预期章节数时，应优先在当前或下一章回收主要伏笔，避免为延长篇幅而无止境新增伏笔。\n");
        } else {
            // 第2章及以后：极简提示，完全沿用第1章已经说明过的全局统一规则、JSON结构和注意事项
            prompt.append("你正在继续为同一部小说规划后续章节（当前为第")
                    .append(chapterNo).append("章）。\n");
            prompt.append("本次生成必须无条件严格遵守你在第1章中已接收的【全局创作要求】，")
                    .append("请继续使用与第1章相同的JSON字段名与结构进行输出。\n");
            prompt.append("如果你对具体格式有任何不确定之处，请参考你在第1章输出的JSON格式，完全复制字段名与结构，只修改本章对应字段的值，")
                    .append("并且不要输出任何解释文字、额外说明或第二个对象。\n");
            prompt.append("当前章节的参考信息：本章预期字数约为").append(effectiveChapterWordCount)
                    .append("字，可在合理范围内微调，以保证叙事完整与节奏自然。\n");
            return prompt.toString();
        }

        // 仅第1章需要明确字段说明、JSON模板和注意事项，后续章节通过上下文沿用
        prompt.append("\n请生成第").append(chapterNo).append("章的以下信息：\n");
        prompt.append("1. chapterTitle：章节标题（简洁有力，吸引读者，避免在标题中使用引号）\n");
        prompt.append("2. corePlot：核心情节概述（200-300字，包含主要冲突和转折）\n");
        prompt.append("3. wordCountEstimate：本章预估字数（以").append(effectiveChapterWordCount).append("字左右为参考，可少量浮动）\n");
        prompt.append("4. storyComplete：故事是否完结（true/false，必须严格遵守上面的收尾规则进行判断）\n");
        prompt.append("5. plots：本章设置的伏笔（可选，数组格式，每个伏笔包含plotName、plotContent和recoveryChapter；")
                .append("当章节数已经接近或超过预期总章节数时，请避免再大量新增需要很久之后才能回收的伏笔）\n");

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他内容：\n");
        prompt.append("{\n");
        prompt.append("  \"chapterTitle\": \"章节标题\",\n");
        prompt.append("  \"corePlot\": \"核心情节概述\",\n");
        prompt.append("  \"wordCountEstimate\": 预估字数,\n");
        prompt.append("  \"storyComplete\": false,\n");
        prompt.append("  \"plots\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"plotName\": \"伏笔名称\",\n");
        prompt.append("      \"plotContent\": \"伏笔内容描述\",\n");
        prompt.append("      \"recoveryChapter\": 预计回收章节数\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("注意事项：\n");
        prompt.append("- 如果这是故事的自然结局，或者已经达到/略超预期章节数且核心矛盾与主要伏笔都能在本章或下一章内收束，请将 storyComplete 设置为 true\n");
        prompt.append("- 如果故事还有清晰且必要的发展空间，并且当前章节号明显低于预期章节数，可以将 storyComplete 设置为 false\n");
        prompt.append("- 字数预估(totalWordCountEstimate)和每章字数(chapterWordCountEstimate)用于指导整体篇幅和收尾时机，不应为了无限延长篇幅而不断增加章节\n");
        prompt.append("- recoveryChapter：伏笔预计在多少章后回收（通常建议在2-4章内回收；当接近或超过预期章节数时，请优先在本章或下一章回收）\n");
        prompt.append("- 伏笔回收要制造惊喜或转折，避免过早揭晓谜底，同时也要避免拖到远超预期章节数才回收，导致故事被不必要地拉长\n");

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

                    // 解析回收章节
                    Object recoveryChapterObj = plotMap.get("recoveryChapter");
                    if (recoveryChapterObj instanceof Number) {
                        int recoveryChapters = ((Number) recoveryChapterObj).intValue();
                        // 将相对章节数转换为绝对章节号
                        long recoveryChapterId = chapterNo + recoveryChapters;
                        plotInfo.setRecoveryChapterId(recoveryChapterId);
                    }

                    plots.add(plotInfo);
                }
                result.setPlots(plots);
            }

            log.info("单章信息解析完成 - 标题: {}, 完结: {}, 字数: {}, 伏笔数: {}",
                     result.getChapterTitle(),
                     result.isStoryComplete(),
                     result.getWordCountEstimate(),
                     result.getPlots() != null ? result.getPlots().size() : 0);

            // 输出伏笔详情
            if (result.getPlots() != null && !result.getPlots().isEmpty()) {
                log.info("本章伏笔详情:");
                for (int i = 0; i < result.getPlots().size(); i++) {
                    PlotInfo plot = result.getPlots().get(i);
                    log.info("  伏笔{}: {} (回收章节: {})",
                            i + 1, plot.getPlotName(),
                            plot.getRecoveryChapterId() != null ? plot.getRecoveryChapterId() : "未指定");
                }
            }

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

                        // 解析回收章节
                        Object recoveryChapterObj = plotMap.get("recoveryChapter");
                        if (recoveryChapterObj instanceof Number) {
                            int recoveryChapters = ((Number) recoveryChapterObj).intValue();
                            // 将相对章节数转换为绝对章节号
                            long recoveryChapterId = chapterNo + recoveryChapters;
                            plotInfo.setRecoveryChapterId(recoveryChapterId);
                        }

                        plots.add(plotInfo);
                    }
                    result.setPlots(plots);
                }

                log.info("清理后解析成功 - 标题: {}, 完结: {}, 字数: {}, 伏笔数: {}",
                         result.getChapterTitle(),
                         result.isStoryComplete(),
                         result.getWordCountEstimate(),
                         result.getPlots() != null ? result.getPlots().size() : 0);

                // 输出伏笔详情
                if (result.getPlots() != null && !result.getPlots().isEmpty()) {
                    log.info("本章伏笔详情:");
                    for (int i = 0; i < result.getPlots().size(); i++) {
                        PlotInfo plot = result.getPlots().get(i);
                        log.info("  伏笔{}: {} (回收章节: {})",
                                i + 1, plot.getPlotName(),
                                plot.getRecoveryChapterId() != null ? plot.getRecoveryChapterId() : "未指定");
                    }
                }

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

        /** 生成本章章节信息时的 response_id，用于本章内容生成 */
        private String responseIdPlan;

        public String getResponseIdPlan() { return responseIdPlan; }
        public void setResponseIdPlan(String responseIdPlan) { this.responseIdPlan = responseIdPlan; }
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