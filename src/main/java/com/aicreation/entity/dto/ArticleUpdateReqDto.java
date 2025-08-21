package com.aicreation.entity.dto;

import com.aicreation.entity.dto.base.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文章更新请求DTO
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Schema(description = "文章更新请求")
public class ArticleUpdateReqDto extends BaseDto {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID", example = "1")
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @Schema(description = "文章名称", example = "AI技术发展趋势")
    @Size(max = 255, message = "文章名称不能超过255个字符")
    private String articleName;

    @Schema(description = "文章简介", example = "本文主要介绍AI技术的发展趋势...")
    @Size(max = 2000, message = "文章简介不能超过2000个字符")
    private String articleOutline;

    @Schema(description = "形象描述", example = "一个充满科技感的未来世界场景...")
    @Size(max = 2000, message = "形象描述不能超过2000个字符")
    private String imageDesc;

    @Schema(description = "音色", example = "alex")
    @Size(max = 100, message = "音色不能超过100个字符")
    private String voiceTone;

    @Schema(description = "语音链接", example = "https://example.com/voice.mp3")
    @Size(max = 500, message = "语音链接不能超过500个字符")
    private String voiceLink;

    @Schema(description = "语音文件地址", example = "/uploads/voice/voice.mp3")
    @Size(max = 500, message = "语音文件地址不能超过500个字符")
    private String voiceFilePath;

    @Schema(description = "视频链接", example = "https://example.com/video.mp4")
    @Size(max = 500, message = "视频链接不能超过500个字符")
    private String videoLink;

    @Schema(description = "视频文件地址", example = "/uploads/video/video.mp4")
    @Size(max = 500, message = "视频文件地址不能超过500个字符")
    private String videoFilePath;

    @Schema(description = "发布状态：1-未发布，2-已发布", example = "1")
    private Integer publishStatus;

    // Getter and Setter methods
    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

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

    public String getImageDesc() {
        return imageDesc;
    }

    public void setImageDesc(String imageDesc) {
        this.imageDesc = imageDesc;
    }

    public String getVoiceTone() {
        return voiceTone;
    }

    public void setVoiceTone(String voiceTone) {
        this.voiceTone = voiceTone;
    }

    public String getVoiceLink() {
        return voiceLink;
    }

    public void setVoiceLink(String voiceLink) {
        this.voiceLink = voiceLink;
    }

    public String getVoiceFilePath() {
        return voiceFilePath;
    }

    public void setVoiceFilePath(String voiceFilePath) {
        this.voiceFilePath = voiceFilePath;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public Integer getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
    }
}
