package com.aicreation.service;

import com.aicreation.entity.dto.MembershipCreateOrderReqDto;
import com.aicreation.entity.dto.MembershipPricingItemRespDto;
import com.aicreation.entity.dto.MembershipPricingConfigSaveReqDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.po.MembershipPricingConfig;
import com.aicreation.entity.po.RechargeOrder;

import java.util.List;

public interface MembershipService {

    /**
     * AI 扣费前校验：非管理员须为有效会员。
     */
    void assertActiveMembershipForAi(Long userId);

    /**
     * 支付成功回调 / 对账：顺延会员并写开通记录（幂等）。
     */
    void applyPaymentSuccess(RechargeOrder order, java.time.LocalDateTime paidAt, String externalTradeNo, String channel);

    /**
     * 退款成功：在满足快照一致时回滚会员结束时间。
     */
    boolean rollbackMembershipForPaymentOrder(Long paymentOrderId, String remark);

    RechargeCreateRespDto createMembershipOrder(MembershipCreateOrderReqDto req);

    List<MembershipPricingItemRespDto> listEnabledPricing();

    List<MembershipPricingConfig> listAllPricingForAdmin();

    void savePricingConfig(MembershipPricingConfigSaveReqDto req);

    /**
     * 管理员赠送/顺延若干月（从当前结束时间顺延，已过期则从当前时间起算）。
     */
    void adminGrantOrExtendMonths(Long targetUserId, int months, String remark);

    /**
     * 管理员指定新的结束时间（须晚于当前结束时间）。
     */
    void adminSetEndTime(Long targetUserId, java.time.LocalDateTime newEndAt, String remark);
}
