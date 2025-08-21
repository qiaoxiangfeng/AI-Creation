package com.aicreation.controller;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.service.IArticleService;
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
@Tag(name = "文章管理", description = "文章相关接口")
@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private IArticleService articleService;

    @Operation(summary = "根据ID查询文章", description = "根据文章ID查询文章详细信息")
    @GetMapping("/{articleId}")
    public BaseResponse<ArticleRespDto> getArticleById(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        ArticleQueryReqDto request = new ArticleQueryReqDto();
        request.setArticleId(articleId);
        ArticleRespDto article = articleService.getArticleById(request);
        return BaseResponse.success(article);
    }

    @Operation(summary = "创建文章", description = "创建新的文章")
    @PostMapping
    public BaseResponse<Long> createArticle(
            @Parameter(description = "文章创建请求") @Valid @RequestBody ArticleCreateReqDto request) {
        Long articleId = articleService.createArticle(request);
        return BaseResponse.success(articleId);
    }

    @Operation(summary = "更新文章", description = "更新文章信息")
    @PutMapping
    public BaseResponse<Boolean> updateArticle(
            @Parameter(description = "文章更新请求") @Valid @RequestBody ArticleUpdateReqDto request) {
        Boolean result = articleService.updateArticle(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除文章", description = "删除指定文章")
    @DeleteMapping
    public BaseResponse<Boolean> deleteArticle(
            @Parameter(description = "文章删除请求") @Valid @RequestBody ArticleDeleteReqDto request) {
        Boolean result = articleService.deleteArticle(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "查询文章列表", description = "分页查询文章列表，支持按名称、音色和发布状态筛选")
    @PostMapping("/list")
    public PageRespDto<ArticleListRespDto> getArticleList(
            @Parameter(description = "文章列表查询请求") @Valid @RequestBody ArticleListReqDto request) {
        return articleService.getArticleList(request);
    }

    @Operation(summary = "更新文章发布状态", description = "更新文章的发布状态")
    @PutMapping("/{articleId}/publish")
    public BaseResponse<Boolean> updateArticlePublishStatus(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "发布状态：1-未发布，2-已发布") @RequestParam Integer publishStatus) {
        Boolean result = articleService.updateArticlePublishStatus(articleId, publishStatus);
        return BaseResponse.success(result);
    }

    @Operation(summary = "根据名称查询文章", description = "根据文章名称查询文章信息")
    @GetMapping("/name/{articleName}")
    public BaseResponse<ArticleRespDto> getArticleByArticleName(
            @Parameter(description = "文章名称") @PathVariable String articleName) {
        ArticleRespDto article = articleService.getArticleByArticleName(articleName);
        return BaseResponse.success(article);
    }
}
