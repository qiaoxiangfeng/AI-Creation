package com.aicreation.controller;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.service.IArticleGenerationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 文章生成配置管理控制器
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Tag(name = "文章生成配置管理", description = "文章生成配置相关接口")
@RestController
@RequestMapping("/article-generation-configs")
public class ArticleGenerationConfigController {

    @Autowired
    private IArticleGenerationConfigService articleGenerationConfigService;

    @Operation(summary = "根据ID查询文章生成配置", description = "根据文章生成配置ID查询文章生成配置详细信息")
    @GetMapping("/{id}")
    public BaseResponse<ArticleGenerationConfigRespDto> getArticleGenerationConfigById(
            @Parameter(description = "文章生成配置ID") @PathVariable Long id) {
        ArticleGenerationConfigQueryReqDto request = new ArticleGenerationConfigQueryReqDto();
        request.setId(id);
        ArticleGenerationConfigRespDto articleGenerationConfig = articleGenerationConfigService.getArticleGenerationConfigById(request);
        return BaseResponse.success(articleGenerationConfig);
    }

    @Operation(summary = "创建文章生成配置", description = "创建新的文章生成配置")
    @PostMapping
    public BaseResponse<Long> createArticleGenerationConfig(
            @Parameter(description = "文章生成配置创建请求") @Valid @RequestBody ArticleGenerationConfigCreateReqDto request) {
        Long articleGenerationConfigId = articleGenerationConfigService.createArticleGenerationConfig(request);
        return BaseResponse.success(articleGenerationConfigId);
    }

    @Operation(summary = "更新文章生成配置", description = "更新文章生成配置信息")
    @PutMapping
    public BaseResponse<Boolean> updateArticleGenerationConfig(
            @Parameter(description = "文章生成配置更新请求") @Valid @RequestBody ArticleGenerationConfigUpdateReqDto request) {
        Boolean result = articleGenerationConfigService.updateArticleGenerationConfig(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除文章生成配置", description = "删除指定文章生成配置")
    @DeleteMapping
    public BaseResponse<Boolean> deleteArticleGenerationConfig(
            @Parameter(description = "文章生成配置删除请求") @Valid @RequestBody ArticleGenerationConfigDeleteReqDto request) {
        Boolean result = articleGenerationConfigService.deleteArticleGenerationConfig(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "查询文章生成配置列表", description = "分页查询文章生成配置列表，支持按分类名称筛选")
    @PostMapping("/list")
    public BaseResponse<PageRespDto<ArticleGenerationConfigListRespDto>> getArticleGenerationConfigList(
            @Parameter(description = "文章生成配置列表查询请求") @Valid @RequestBody ArticleGenerationConfigListReqDto request) {
        PageRespDto<ArticleGenerationConfigListRespDto> page = articleGenerationConfigService.getArticleGenerationConfigList(request);
        return BaseResponse.success(page);
    }

    @Operation(summary = "根据主题查询文章生成配置", description = "根据文章生成配置主题查询文章生成配置信息")
    @GetMapping("/theme/{theme}")
    public BaseResponse<ArticleGenerationConfigRespDto> getArticleGenerationConfigByTheme(
            @Parameter(description = "文章生成配置主题") @PathVariable String theme) {
        ArticleGenerationConfigRespDto articleGenerationConfig = articleGenerationConfigService.getArticleGenerationConfigByTheme(theme);
        return BaseResponse.success(articleGenerationConfig);
    }
}