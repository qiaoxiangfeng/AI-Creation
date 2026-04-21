package com.aicreation.service;

import com.aicreation.entity.dto.RechargeCreateReqDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.dto.WalletBalanceRespDto;
import com.aicreation.entity.dto.AdminAddBalanceReqDto;
import com.aicreation.entity.po.RechargeOrder;
import com.aicreation.entity.po.WalletLedger;

import java.util.List;

public interface WalletService {
    WalletBalanceRespDto getCurrentUserBalance();

    List<WalletLedger> listCurrentUserLedger();

    RechargeCreateRespDto createRechargeOrder(RechargeCreateReqDto req);

    RechargeOrder getRechargeOrder(String orderNo);

    /**
     * 管理员手动补入用户余额（用于线下转账补记）
     */
    boolean adminAddBalance(Long userId, AdminAddBalanceReqDto req);

    /**
     * 超时关闭 + 支付对账补偿（支付宝“交易查询”）。
     * 用于兜底：避免回调丢失导致订单不入账。
     */
    void closeAndReconcileExpiredRechargeOrders(int limit);

    /**
     * 支付宝回调入账（幂等）。
     * 返回 true 表示成功处理（含幂等重复）。
     */
    boolean handleAlipayNotify(java.util.Map<String, String> params, String rawPayload);

    /**
     * 微信回调入账（幂等）。
     * 返回 true 表示成功处理（含幂等重复）。
     */
    boolean handleWeChatNotify(java.util.Map<String, String> params, String rawPayload);
}

