package com.aicreation.entity.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户钱包表
 */
public class UserWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;

    /**
     * 总余额（分）
     */
    private Long totalBalanceCent;

    /**
     * 冻结余额（分）
     */
    private Long frozenBalanceCent;

    /**
     * 可用余额（分）
     */
    private Long availableBalanceCent;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 删除标记（1-有效，0-无效）
     */
    private Integer resState;

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

    public Long getTotalBalanceCent() {
        return totalBalanceCent;
    }

    public void setTotalBalanceCent(Long totalBalanceCent) {
        this.totalBalanceCent = totalBalanceCent;
    }

    public Long getFrozenBalanceCent() {
        return frozenBalanceCent;
    }

    public void setFrozenBalanceCent(Long frozenBalanceCent) {
        this.frozenBalanceCent = frozenBalanceCent;
    }

    public Long getAvailableBalanceCent() {
        return availableBalanceCent;
    }

    public void setAvailableBalanceCent(Long availableBalanceCent) {
        this.availableBalanceCent = availableBalanceCent;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getResState() {
        return resState;
    }

    public void setResState(Integer resState) {
        this.resState = resState;
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

