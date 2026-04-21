package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 使用计费表
 */
public class AiUsageBilling implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long articleId;
    private Long chapterId;
    private String bizScene;
    private String provider;
    private String modelName;
    private Integer requestTokens;
    private Integer responseTokens;
    private Integer totalTokens;
    private Long estimatedCostCent;
    private Long actualCostCent;
    private Long preAuthAmountCent;
    private Long settledAmountCent;
    private Long refundAmountCent;
    private String status;
    private String traceId;
    private String idempotencyKey;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getBizScene() {
        return bizScene;
    }

    public void setBizScene(String bizScene) {
        this.bizScene = bizScene;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getRequestTokens() {
        return requestTokens;
    }

    public void setRequestTokens(Integer requestTokens) {
        this.requestTokens = requestTokens;
    }

    public Integer getResponseTokens() {
        return responseTokens;
    }

    public void setResponseTokens(Integer responseTokens) {
        this.responseTokens = responseTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getEstimatedCostCent() {
        return estimatedCostCent;
    }

    public void setEstimatedCostCent(Long estimatedCostCent) {
        this.estimatedCostCent = estimatedCostCent;
    }

    public Long getActualCostCent() {
        return actualCostCent;
    }

    public void setActualCostCent(Long actualCostCent) {
        this.actualCostCent = actualCostCent;
    }

    public Long getPreAuthAmountCent() {
        return preAuthAmountCent;
    }

    public void setPreAuthAmountCent(Long preAuthAmountCent) {
        this.preAuthAmountCent = preAuthAmountCent;
    }

    public Long getSettledAmountCent() {
        return settledAmountCent;
    }

    public void setSettledAmountCent(Long settledAmountCent) {
        this.settledAmountCent = settledAmountCent;
    }

    public Long getRefundAmountCent() {
        return refundAmountCent;
    }

    public void setRefundAmountCent(Long refundAmountCent) {
        this.refundAmountCent = refundAmountCent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

