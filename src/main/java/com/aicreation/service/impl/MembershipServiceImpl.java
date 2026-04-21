package com.aicreation.service.impl;

import com.aicreation.constant.MembershipSubscriptionSource;
import com.aicreation.constant.MembershipTier;
import com.aicreation.constant.PaymentOrderBizType;
import com.aicreation.entity.dto.MembershipCreateOrderReqDto;
import com.aicreation.entity.dto.MembershipPricingConfigSaveReqDto;
import com.aicreation.entity.dto.MembershipPricingItemRespDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.po.MembershipPricingConfig;
import com.aicreation.entity.po.MembershipSubscription;
import com.aicreation.entity.po.RechargeOrder;
import com.aicreation.entity.po.User;
import com.aicreation.entity.po.UserNotification;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.external.AlipayClient;
import com.aicreation.external.WeChatClient;
import com.aicreation.mapper.MembershipPricingConfigMapper;
import com.aicreation.mapper.MembershipSubscriptionMapper;
import com.aicreation.mapper.RechargeOrderMapper;
import com.aicreation.mapper.UserMapper;
import com.aicreation.mapper.UserNotificationMapper;
import com.aicreation.security.CurrentUserHolder;
import com.aicreation.service.MembershipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MembershipServiceImpl implements MembershipService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MembershipPricingConfigMapper membershipPricingConfigMapper;

    @Autowired
    private MembershipSubscriptionMapper membershipSubscriptionMapper;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private WeChatClient weChatClient;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Override
    public void assertActiveMembershipForAi(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
        }
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        LocalDateTime end = user.getMembershipEndAt();
        String tier = user.getMembershipTier();
        if (!StringUtils.hasText(tier) || MembershipTier.NONE.equalsIgnoreCase(tier) || end == null) {
            throw new BusinessException(ErrorCodeEnum.MEMBERSHIP_REQUIRED);
        }
        if (!end.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCodeEnum.MEMBERSHIP_EXPIRED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPaymentSuccess(RechargeOrder order, LocalDateTime paidAt, String externalTradeNo, String channel) {
        if (order == null || order.getId() == null) {
            return;
        }
        MembershipSubscription existed = membershipSubscriptionMapper.selectByPaymentOrderId(order.getId());
        if (existed != null) {
            return;
        }
        if (!PaymentOrderBizType.MEMBERSHIP.equals(order.getBizType())) {
            return;
        }
        Integer months = order.getMembershipDurationMonths();
        if (months == null || months <= 0) {
            log.error("会员订单缺少时长：orderId={}", order.getId());
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "会员订单数据不完整");
        }

        User user = userMapper.selectByPrimaryKeyForUpdate(order.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LocalDateTime previousEnd = user.getMembershipEndAt();
        LocalDateTime newEnd = computeNewEnd(previousEnd, paidAt, months);

        LocalDateTime startAt = user.getMembershipStartAt();
        if (startAt == null || MembershipTier.NONE.equalsIgnoreCase(user.getMembershipTier())) {
            startAt = paidAt;
        }

        User upd = new User();
        upd.setId(user.getId());
        upd.setMembershipTier(MembershipTier.BASIC);
        upd.setMembershipStartAt(startAt);
        upd.setMembershipEndAt(newEnd);
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(upd);

        MembershipSubscription sub = new MembershipSubscription();
        sub.setUserId(order.getUserId());
        sub.setSource(MembershipSubscriptionSource.PAYMENT);
        sub.setPaymentOrderId(order.getId());
        sub.setTier(MembershipTier.BASIC);
        sub.setDurationMonths(months);
        sub.setPreviousEndAt(previousEnd);
        sub.setNewEndAt(newEnd);
        sub.setAmountCent(order.getAmountCent() == null ? 0L : order.getAmountCent());
        sub.setChannel(channel);
        sub.setExternalTradeNo(externalTradeNo);
        sub.setCreateTime(LocalDateTime.now());
        membershipSubscriptionMapper.insert(sub);

        UserNotification n = new UserNotification();
        n.setUserId(order.getUserId());
        n.setType("MEMBERSHIP_PURCHASED");
        n.setTitle("会员开通成功");
        n.setContent("会员已顺延至 " + formatEnd(newEnd) + "，祝您使用愉快。");
        n.setIsRead(false);
        n.setBizRefType("MEMBERSHIP_SUBSCRIPTION");
        n.setBizRefId(sub.getId());
        n.setCreateTime(LocalDateTime.now());
        n.setUpdateTime(LocalDateTime.now());
        userNotificationMapper.insert(n);
    }

    private static String formatEnd(LocalDateTime t) {
        return t == null ? "-" : t.toString().replace('T', ' ');
    }

    static LocalDateTime computeNewEnd(LocalDateTime previousEnd, LocalDateTime paidAt, int months) {
        if (months <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "months无效");
        }
        if (previousEnd == null || !previousEnd.isAfter(paidAt)) {
            return paidAt.plusMonths(months);
        }
        return previousEnd.plusMonths(months);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackMembershipForPaymentOrder(Long paymentOrderId, String remark) {
        if (paymentOrderId == null) {
            return false;
        }
        MembershipSubscription sub = membershipSubscriptionMapper.selectByPaymentOrderId(paymentOrderId);
        if (sub == null) {
            return false;
        }
        User user = userMapper.selectByPrimaryKeyForUpdate(sub.getUserId());
        if (user == null) {
            return false;
        }
        LocalDateTime currentEnd = user.getMembershipEndAt();
        LocalDateTime snapNew = sub.getNewEndAt();
        if (currentEnd == null || snapNew == null || !currentEnd.isEqual(snapNew)) {
            log.warn("会员退款回滚跳过：结束时间与快照不一致 userId={}, paymentOrderId={}", user.getId(), paymentOrderId);
            return false;
        }

        User upd = new User();
        upd.setId(user.getId());
        upd.setMembershipEndAt(sub.getPreviousEndAt());
        if (sub.getPreviousEndAt() == null) {
            upd.setMembershipTier(MembershipTier.NONE);
        }
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(upd);

        MembershipSubscription rollbackRow = new MembershipSubscription();
        rollbackRow.setUserId(sub.getUserId());
        rollbackRow.setSource(MembershipSubscriptionSource.REFUND_ROLLBACK);
        rollbackRow.setPaymentOrderId(paymentOrderId);
        rollbackRow.setTier(sub.getTier());
        rollbackRow.setDurationMonths(sub.getDurationMonths());
        rollbackRow.setPreviousEndAt(snapNew);
        rollbackRow.setNewEndAt(sub.getPreviousEndAt());
        rollbackRow.setAmountCent(0L);
        rollbackRow.setRemark(remark);
        rollbackRow.setCreateTime(LocalDateTime.now());
        membershipSubscriptionMapper.insert(rollbackRow);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeCreateRespDto createMembershipOrder(MembershipCreateOrderReqDto req) {
        Long userId = CurrentUserHolder.requireUserId();
        if (req == null || req.getPricingConfigId() == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        MembershipPricingConfig cfg = membershipPricingConfigMapper.selectByPrimaryKey(req.getPricingConfigId());
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
            throw new BusinessException(ErrorCodeEnum.MEMBERSHIP_PRICING_NOT_FOUND);
        }
        long priceCent = computePriceCent(cfg);
        String channel = StringUtils.hasText(req.getChannel()) ? req.getChannel().trim().toUpperCase() : "ALIPAY";
        if (!"ALIPAY".equals(channel) && !"WECHAT".equals(channel)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "channel 仅支持 ALIPAY/WECHAT");
        }

        String orderNo = "MS" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        LocalDateTime now = LocalDateTime.now();
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setChannel(channel);
        order.setAmountCent(priceCent);
        order.setStatus("CREATED");
        order.setSubject("基础会员 " + cfg.getDurationMonths() + " 个月");
        order.setExpireTime(now.plusMinutes(15));
        order.setIdempotencyKey("membership-create:" + userId + ":" + orderNo);
        order.setBizType(PaymentOrderBizType.MEMBERSHIP);
        order.setMembershipPricingConfigId(cfg.getId());
        order.setMembershipDurationMonths(cfg.getDurationMonths());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        rechargeOrderMapper.insert(order);

        String payUrl;
        if ("ALIPAY".equals(channel)) {
            payUrl = alipayClient.precreate(orderNo, priceCent, order.getSubject());
        } else {
            payUrl = weChatClient.createNativePay(orderNo, priceCent, order.getSubject());
        }
        RechargeOrder upd = new RechargeOrder();
        upd.setId(order.getId());
        upd.setStatus("PAYING");
        upd.setPayUrl(payUrl);
        upd.setUpdateTime(LocalDateTime.now());
        rechargeOrderMapper.updateByPrimaryKey(upd);

        RechargeCreateRespDto resp = new RechargeCreateRespDto();
        resp.setOrderNo(orderNo);
        resp.setPayUrl(payUrl);
        resp.setExpireTime(order.getExpireTime());
        return resp;
    }

    @Override
    public List<MembershipPricingItemRespDto> listEnabledPricing() {
        List<MembershipPricingConfig> list =
                membershipPricingConfigMapper.selectEnabledByTier(MembershipTier.BASIC);
        List<MembershipPricingItemRespDto> out = new ArrayList<>();
        if (list == null) {
            return out;
        }
        for (MembershipPricingConfig c : list) {
            MembershipPricingItemRespDto d = new MembershipPricingItemRespDto();
            d.setId(c.getId());
            d.setTier(c.getTier());
            d.setDurationMonths(c.getDurationMonths());
            d.setBaseMonthPriceCent(c.getBaseMonthPriceCent());
            d.setDiscountRate(c.getDiscountRate());
            d.setPriceCent(computePriceCent(c));
            out.add(d);
        }
        return out;
    }

    @Override
    public List<MembershipPricingConfig> listAllPricingForAdmin() {
        List<MembershipPricingConfig> list = membershipPricingConfigMapper.selectAllForAdmin();
        return list == null ? List.of() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePricingConfig(MembershipPricingConfigSaveReqDto req) {
        LocalDateTime now = LocalDateTime.now();
        if (req.getId() == null) {
            MembershipPricingConfig row = new MembershipPricingConfig();
            row.setTier(req.getTier().trim());
            row.setDurationMonths(req.getDurationMonths());
            row.setBaseMonthPriceCent(req.getBaseMonthPriceCent());
            row.setDiscountRate(req.getDiscountRate());
            row.setEnabled(req.getEnabled());
            row.setSortOrder(req.getSortOrder());
            row.setCreateTime(now);
            row.setUpdateTime(now);
            membershipPricingConfigMapper.insert(row);
        } else {
            MembershipPricingConfig existing = membershipPricingConfigMapper.selectByPrimaryKey(req.getId());
            if (existing == null) {
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            MembershipPricingConfig row = new MembershipPricingConfig();
            row.setId(req.getId());
            row.setTier(req.getTier().trim());
            row.setDurationMonths(req.getDurationMonths());
            row.setBaseMonthPriceCent(req.getBaseMonthPriceCent());
            row.setDiscountRate(req.getDiscountRate());
            row.setEnabled(req.getEnabled());
            row.setSortOrder(req.getSortOrder());
            row.setUpdateTime(now);
            membershipPricingConfigMapper.updateByPrimaryKey(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminGrantOrExtendMonths(Long targetUserId, int months, String remark) {
        if (targetUserId == null || months <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        LocalDateTime paidAt = LocalDateTime.now();
        User user = userMapper.selectByPrimaryKeyForUpdate(targetUserId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        LocalDateTime previousEnd = user.getMembershipEndAt();
        LocalDateTime newEnd = computeNewEnd(previousEnd, paidAt, months);
        LocalDateTime startAt = user.getMembershipStartAt();
        if (startAt == null || MembershipTier.NONE.equalsIgnoreCase(user.getMembershipTier())) {
            startAt = paidAt;
        }
        User upd = new User();
        upd.setId(user.getId());
        upd.setMembershipTier(MembershipTier.BASIC);
        upd.setMembershipStartAt(startAt);
        upd.setMembershipEndAt(newEnd);
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(upd);

        MembershipSubscription sub = new MembershipSubscription();
        sub.setUserId(targetUserId);
        sub.setSource(MembershipSubscriptionSource.ADMIN_GRANT);
        sub.setTier(MembershipTier.BASIC);
        sub.setDurationMonths(months);
        sub.setPreviousEndAt(previousEnd);
        sub.setNewEndAt(newEnd);
        sub.setAmountCent(0L);
        sub.setRemark(remark);
        sub.setCreateTime(LocalDateTime.now());
        membershipSubscriptionMapper.insert(sub);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSetEndTime(Long targetUserId, LocalDateTime newEndAt, String remark) {
        if (targetUserId == null || newEndAt == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User user = userMapper.selectByPrimaryKeyForUpdate(targetUserId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        LocalDateTime previousEnd = user.getMembershipEndAt();
        if (previousEnd != null) {
            if (!newEndAt.isAfter(previousEnd)) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "新结束时间须晚于当前结束时间");
            }
        } else if (!newEndAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "新结束时间须晚于当前时间");
        }
        User upd = new User();
        upd.setId(user.getId());
        upd.setMembershipTier(MembershipTier.BASIC);
        if (user.getMembershipStartAt() == null) {
            upd.setMembershipStartAt(LocalDateTime.now());
        }
        upd.setMembershipEndAt(newEndAt);
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(upd);

        MembershipSubscription sub = new MembershipSubscription();
        sub.setUserId(targetUserId);
        sub.setSource(MembershipSubscriptionSource.ADMIN_EXTEND);
        sub.setTier(MembershipTier.BASIC);
        sub.setPreviousEndAt(previousEnd);
        sub.setNewEndAt(newEndAt);
        sub.setAmountCent(0L);
        sub.setRemark(remark);
        sub.setCreateTime(LocalDateTime.now());
        membershipSubscriptionMapper.insert(sub);
    }

    public static long computePriceCent(MembershipPricingConfig c) {
        Objects.requireNonNull(c);
        int dm = c.getDurationMonths() == null ? 0 : c.getDurationMonths();
        long base = c.getBaseMonthPriceCent() == null ? 0L : c.getBaseMonthPriceCent();
        BigDecimal rate = c.getDiscountRate() == null ? BigDecimal.ONE : c.getDiscountRate();
        BigDecimal raw = BigDecimal.valueOf(base)
                .multiply(BigDecimal.valueOf(dm))
                .multiply(rate);
        return raw.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
