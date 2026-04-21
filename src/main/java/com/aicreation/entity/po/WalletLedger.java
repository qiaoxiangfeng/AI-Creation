package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 钱包流水表
 */
public class WalletLedger implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String bizType;
    private String direction;
    private Long amountCent;
    private Long balanceBeforeCent;
    private Long balanceAfterCent;
    private String relatedBizType;
    private Long relatedBizId;
    private String idempotencyKey;
    private String remark;
    private LocalDateTime createTime;

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

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public Long getBalanceBeforeCent() {
        return balanceBeforeCent;
    }

    public void setBalanceBeforeCent(Long balanceBeforeCent) {
        this.balanceBeforeCent = balanceBeforeCent;
    }

    public Long getBalanceAfterCent() {
        return balanceAfterCent;
    }

    public void setBalanceAfterCent(Long balanceAfterCent) {
        this.balanceAfterCent = balanceAfterCent;
    }

    public String getRelatedBizType() {
        return relatedBizType;
    }

    public void setRelatedBizType(String relatedBizType) {
        this.relatedBizType = relatedBizType;
    }

    public Long getRelatedBizId() {
        return relatedBizId;
    }

    public void setRelatedBizId(Long relatedBizId) {
        this.relatedBizId = relatedBizId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

