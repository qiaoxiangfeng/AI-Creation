package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.PageReqDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

/**
 * 文章列表查询请求DTO
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章列表查询请求")
public class ArticleListReqDto extends PageReqDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章名称搜索关键词（可选）", example = "AI技术")
    @Size(max = 100, message = "文章名称搜索关键词不能超过100个字符")
    private String articleName;

    @Schema(description = "音色筛选（可选）", example = "alex")
    @Size(max = 100, message = "音色不能超过100个字符")
    private String voiceTone;

    @Schema(description = "发布状态筛选（可选）：1-未发布，2-已发布", example = "1")
    private Integer publishStatus;

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getVoiceTone() {
        return voiceTone;
    }

    public void setVoiceTone(String voiceTone) {
        this.voiceTone = voiceTone;
    }

    public Integer getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
    }
}
