package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MembershipSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String source;
    private Long paymentOrderId;
    private String tier;
    private Integer durationMonths;
    private LocalDateTime previousEndAt;
    private LocalDateTime newEndAt;
    private Long amountCent;
    private String channel;
    private String externalTradeNo;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getPaymentOrderId() {
        return paymentOrderId;
    }

    public void setPaymentOrderId(Long paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public LocalDateTime getPreviousEndAt() {
        return previousEndAt;
    }

    public void setPreviousEndAt(LocalDateTime previousEndAt) {
        this.previousEndAt = previousEndAt;
    }

    public LocalDateTime getNewEndAt() {
        return newEndAt;
    }

    public void setNewEndAt(LocalDateTime newEndAt) {
        this.newEndAt = newEndAt;
    }

    public Long getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Long amountCent) {
        this.amountCent = amountCent;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getExternalTradeNo() {
        return externalTradeNo;
    }

    public void setExternalTradeNo(String externalTradeNo) {
        this.externalTradeNo = externalTradeNo;
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
