package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标题生成去重：已存在文章摘要信息
 */
@Schema(description = "标题生成去重：已存在文章信息（标题+大纲）")
public class ArticleTitleDedupItemDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章标题")
    private String articleName;

    @Schema(description = "文章大纲")
    private String articleOutline;

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getArticleOutline() {
        return articleOutline;
    }

    public void setArticleOutline(String articleOutline) {
        this.articleOutline = articleOutline;
    }
}

