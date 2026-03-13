package com.aicreation.controller;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.entity.dto.ArticleQueryReqDto;
import com.aicreation.service.IArticleService;
import com.aicreation.generate.ArticleTitleGenerator;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 文章管理控制器
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@RestController
@RequestMapping("/articles")
@Tag(name = "文章管理", description = "文章相关的管理接口")
public class ArticleController {

    @Autowired
    private IArticleService articleService;

    @Autowired
    private ArticleTitleGenerator articleTitleGenerator;

    @Autowired
    private com.aicreation.generate.ArticleContentGenerator articleContentGenerator;

    @Operation(summary = "查询文章列表", description = "分页查询文章列表，支持按名称、类型、发布状态筛选")
    @PostMapping("/list")
    public BaseResponse<PageRespDto<ArticleListRespDto>> getArticleList(@RequestBody ArticleListReqDto request) {
        PageRespDto<ArticleListRespDto> result = articleService.getArticleList(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "创建文章", description = "创建新文章")
    @PostMapping
    public BaseResponse<Long> createArticle(@Valid @RequestBody ArticleCreateReqDto request) {
        Long articleId = articleService.createArticle(request);
        return BaseResponse.success(articleId);
    }

    @Operation(summary = "更新文章", description = "更新文章信息")
    @PutMapping
    public BaseResponse<Boolean> updateArticle(@Valid @RequestBody ArticleUpdateReqDto request) {
        Boolean result = articleService.updateArticle(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "根据ID查询文章", description = "根据文章ID查询文章详细信息")
    @GetMapping("/{articleId}")
    public BaseResponse<ArticleRespDto> getArticleById(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        ArticleQueryReqDto request = new ArticleQueryReqDto();
        request.setArticleId(articleId);
        ArticleRespDto article = articleService.getArticleById(request);
        return BaseResponse.success(article);
    }

    @Operation(summary = "根据文章名称查询文章", description = "根据文章名称查询文章信息")
    @GetMapping("/name/{articleName}")
    public BaseResponse<ArticleRespDto> getArticleByArticleName(
            @Parameter(description = "文章名称") @PathVariable String articleName) {
        ArticleRespDto article = articleService.getArticleByArticleName(articleName);
        return BaseResponse.success(article);
    }

    @Operation(summary = "获取文章章节列表", description = "根据文章ID获取该文章的所有章节列表")
    @GetMapping("/{articleId}/chapters")
    public BaseResponse<List<ArticleChapterRespDto>> getArticleChapters(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        List<ArticleChapterRespDto> chapters = articleService.getArticleChapters(articleId);
        return BaseResponse.success(chapters);
    }

    @Operation(summary = "获取文章完整文本", description = "根据文章ID获取包含所有章节的完整文本内容")
    @GetMapping("/{articleId}/full-text")
    public BaseResponse<String> getArticleFullText(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        String fullText = articleService.getArticleFullText(articleId);
        return BaseResponse.success(fullText);
    }

    @Operation(summary = "触发文章内容生成", description = "手动触发指定文章的内容生成任务")
    @PostMapping("/{articleId}/generate-content")
    public BaseResponse<Boolean> generateArticleContent(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        boolean result = articleService.generateArticleContent(articleId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "获取文章生成进度", description = "获取指定文章的生成进度信息")
    @GetMapping("/{articleId}/progress")
    public BaseResponse<ArticleProgressDto> getArticleProgress(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        ArticleProgressDto progress = articleService.getArticleProgress(articleId);
        return BaseResponse.success(progress);
    }

    @Operation(summary = "更新章节信息", description = "更新章节的核心剧情、字数预估和伏笔信息")
    @PutMapping("/chapters/{chapterId}")
    public BaseResponse<Boolean> updateChapterInfo(
            @Parameter(description = "章节ID") @PathVariable Long chapterId,
            @Parameter(description = "章节信息更新请求") @Valid @RequestBody ChapterUpdateReqDto request) {
        Boolean result = articleService.updateChapterInfo(chapterId, request.getCorePlot(), request.getWordCountEstimate(), request.getPlots());
        return BaseResponse.success(result);
    }


    @Operation(summary = "删除章节", description = "删除指定的章节及其关联信息")
    @DeleteMapping("/chapters/{chapterId}")
    public BaseResponse<Boolean> deleteChapter(
            @Parameter(description = "章节ID") @PathVariable Long chapterId) {
        Boolean result = articleService.deleteChapter(chapterId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除文章", description = "删除指定文章及其关联章节（级联软删除）")
    @DeleteMapping
    public BaseResponse<Boolean> deleteArticle(@Valid @RequestBody ArticleDeleteReqDto request) {
        Boolean result = articleService.deleteArticle(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除文章（按ID）", description = "按文章ID删除文章及其关联章节（级联软删除）")
    @DeleteMapping("/{articleId}")
    public BaseResponse<Boolean> deleteArticleById(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        ArticleDeleteReqDto request = new ArticleDeleteReqDto();
        request.setArticleId(articleId);
        Boolean result = articleService.deleteArticle(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "生成文章章节", description = "为指定文章生成章节信息")
    @PostMapping("/{articleId}/generate-chapters")
    public BaseResponse<Boolean> generateArticleChapters(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        Boolean result = articleService.generateArticleChapters(articleId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "生成文章章节内容", description = "为指定文章生成章节内容")
    @PostMapping("/{articleId}/generate-chapter-content")
    public BaseResponse<Boolean> generateArticleChapterContent(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        Boolean result = articleService.generateArticleChapterContent(articleId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "生成单个章节内容", description = "为指定章节生成内容")
    @PostMapping("/{articleId}/generate-chapter-content/{chapterId}")
    public BaseResponse<Boolean> generateChapterContent(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "章节ID") @PathVariable Long chapterId) {
        try {
            articleContentGenerator.generateChapterContent(chapterId);
            return BaseResponse.success(true);
        } catch (Exception e) {
            return BaseResponse.error("67999999", "生成章节内容失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重新生成单个章节内容", description = "根据用户修改意见重新生成指定章节内容")
    @PostMapping("/{articleId}/generate-chapter-content/{chapterId}/regenerate")
    public BaseResponse<Boolean> regenerateChapterContent(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "章节ID") @PathVariable Long chapterId,
            @Parameter(description = "章节重新生成请求") @Valid @RequestBody ChapterRegenerateReqDto request) {
        try {
            articleContentGenerator.regenerateChapterContent(chapterId, request.getInstruction());
            return BaseResponse.success(true);
        } catch (Exception e) {
            return BaseResponse.error("67999999", "重新生成章节内容失败: " + e.getMessage());
        }
    }

    @Operation(summary = "生成单个文章标题", description = "根据文章生成配置生成一个新的文章标题")
    @PostMapping("/generate-title/{configId}")
    public BaseResponse<Long> generateSingleTitle(
            @Parameter(description = "文章生成配置ID") @PathVariable Long configId) {
        try {
            Long articleId = articleTitleGenerator.generateSingleTitle(configId);
            return BaseResponse.success(articleId);
        } catch (Exception e) {
            return BaseResponse.error("67999999", "生成文章标题失败: " + e.getMessage());
        }
    }

}