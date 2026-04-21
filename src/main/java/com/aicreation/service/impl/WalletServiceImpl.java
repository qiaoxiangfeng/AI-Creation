package com.aicreation.service.impl;

import com.aicreation.constant.PaymentOrderBizType;
import com.aicreation.entity.dto.RechargeCreateReqDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.dto.AdminAddBalanceReqDto;
import com.aicreation.entity.dto.WalletBalanceRespDto;
import com.aicreation.entity.po.RechargeOrder;
import com.aicreation.entity.po.UserNotification;
import com.aicreation.entity.po.UserWallet;
import com.aicreation.entity.po.WalletLedger;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.external.AlipayClient;
import com.aicreation.external.WeChatClient;
import com.aicreation.mapper.UserMapper;
import com.aicreation.mapper.RechargeOrderMapper;
import com.aicreation.mapper.UserNotificationMapper;
import com.aicreation.mapper.UserWalletMapper;
import com.aicreation.mapper.WalletLedgerMapper;
import com.aicreation.security.CurrentUserHolder;
import com.aicreation.service.MembershipService;
import com.aicreation.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    @Autowired
    private UserWalletMapper userWalletMapper;

    @Autowired
    private WalletLedgerMapper walletLedgerMapper;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatClient weChatClient;

    @Autowired
    private MembershipService membershipService;

    @Override
    public WalletBalanceRespDto getCurrentUserBalance() {
        Long userId = CurrentUserHolder.requireUserId();
        UserWallet wallet = ensureWallet(userId);
        WalletBalanceRespDto dto = new WalletBalanceRespDto();
        dto.setTotalBalanceCent(nvl(wallet.getTotalBalanceCent()));
        dto.setFrozenBalanceCent(nvl(wallet.getFrozenBalanceCent()));
        dto.setAvailableBalanceCent(nvl(wallet.getAvailableBalanceCent()));
        return dto;
    }

    @Override
    public List<WalletLedger> listCurrentUserLedger() {
        Long userId = CurrentUserHolder.requireUserId();
        return walletLedgerMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeCreateRespDto createRechargeOrder(RechargeCreateReqDto req) {
        Long userId = CurrentUserHolder.requireUserId();
        if (req == null || req.getAmountCent() == null || req.getAmountCent() <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String channel = StringUtils.hasText(req.getChannel()) ? req.getChannel().trim().toUpperCase() : "ALIPAY";
        if (!"ALIPAY".equals(channel) && !"WECHAT".equals(channel)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "channel 仅支持 ALIPAY/WECHAT");
        }

        String orderNo = "RC" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        LocalDateTime now = LocalDateTime.now();
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setChannel(channel);
        order.setAmountCent(req.getAmountCent());
        order.setStatus("CREATED");
        order.setSubject("余额充值");
        order.setExpireTime(now.plusMinutes(15));
        order.setIdempotencyKey("recharge-create:" + userId + ":" + orderNo);
        order.setBizType(PaymentOrderBizType.RECHARGE);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        rechargeOrderMapper.insert(order);

        // 预下单生成支付内容（二维码/跳转 URL）
        String payUrl;
        if ("ALIPAY".equals(channel)) {
            payUrl = alipayClient.precreate(orderNo, req.getAmountCent(), "余额充值");
        } else {
            payUrl = weChatClient.createNativePay(orderNo, req.getAmountCent(), "余额充值");
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
    public RechargeOrder getRechargeOrder(String orderNo) {
        Long userId = CurrentUserHolder.requireUserId();
        if (!StringUtils.hasText(orderNo)) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo.trim());
        if (order == null) throw new BusinessException(ErrorCodeEnum.RECHARGE_ORDER_NOT_FOUND);
        if (!Objects.equals(userId, order.getUserId())) throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminAddBalance(Long userId, AdminAddBalanceReqDto req) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (req == null || req.getAmountCent() == null || req.getAmountCent() <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 仅用于防止管理员误操作不存在用户
        if (userMapper.selectByPrimaryKey(userId) == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        String idempotencyKey = StringUtils.hasText(req.getIdempotencyKey())
                ? req.getIdempotencyKey().trim()
                : ("admin-add-balance:" + userId + ":" + System.currentTimeMillis() + ":" + (int) (Math.random() * 1000));

        WalletLedger existing = walletLedgerMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();

        UserWallet wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            // 理论上存在用户就应已创建钱包，但兼容旧数据：先补建再落账
            wallet = new UserWallet();
            wallet.setUserId(userId);
            wallet.setTotalBalanceCent(0L);
            wallet.setFrozenBalanceCent(0L);
            wallet.setAvailableBalanceCent(0L);
            wallet.setVersion(0);
            wallet.setResState(1);
            wallet.setCreateTime(now);
            wallet.setUpdateTime(now);
            userWalletMapper.insert(wallet);
            wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        }

        long beforeTotal = wallet.getTotalBalanceCent() == null ? 0L : wallet.getTotalBalanceCent();
        long beforeFrozen = wallet.getFrozenBalanceCent() == null ? 0L : wallet.getFrozenBalanceCent();
        long afterTotal = beforeTotal + req.getAmountCent();
        long afterFrozen = beforeFrozen;
        long afterAvailable = afterTotal - afterFrozen;

        UserWallet updWallet = new UserWallet();
        updWallet.setId(wallet.getId());
        updWallet.setTotalBalanceCent(afterTotal);
        updWallet.setFrozenBalanceCent(afterFrozen);
        updWallet.setAvailableBalanceCent(afterAvailable);
        updWallet.setVersion((wallet.getVersion() == null ? 0 : wallet.getVersion()) + 1);
        updWallet.setResState(wallet.getResState() == null ? 1 : wallet.getResState());
        updWallet.setUpdateTime(now);
        userWalletMapper.updateByPrimaryKey(updWallet);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setBizType("MANUAL_ADJUST");
        ledger.setDirection("IN");
        ledger.setAmountCent(req.getAmountCent());
        ledger.setBalanceBeforeCent(beforeTotal);
        ledger.setBalanceAfterCent(afterTotal);
        ledger.setRelatedBizType("MANUAL_WALLET_ADD");
        ledger.setRelatedBizId(null);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setRemark(req.getRemark());
        ledger.setCreateTime(now);
        walletLedgerMapper.insert(ledger);

        // 通知用户（用于前端通知铃铛展示）
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setType("RECHARGE_SUCCESS");
        n.setTitle("余额已到账");
        double amountYuan = req.getAmountCent() / 100.0;
        n.setContent("管理员已补入余额：" + amountYuan + " 元" + (StringUtils.hasText(req.getRemark()) ? ("（" + req.getRemark().trim() + "）") : ""));
        n.setIsRead(false);
        n.setBizRefType("MANUAL_WALLET_ADD");
        n.setBizRefId(null);
        n.setCreateTime(now);
        n.setUpdateTime(now);
        userNotificationMapper.insert(n);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAndReconcileExpiredRechargeOrders(int limit) {
        if (limit <= 0) return;

        LocalDateTime now = LocalDateTime.now();

        // 1) 超时关闭：到期且未入账订单置为 CLOSED
        rechargeOrderMapper.markExpiredUnpaidOrdersAsClosed(now);

        // 2) 对账补偿：到期后再等一段时间，避免过早查询造成无意义压力
        LocalDateTime beforeTime = now.minusMinutes(5);
        List<RechargeOrder> orders = rechargeOrderMapper.selectExpiredUnpaidOrders(beforeTime, limit);
        if (orders == null || orders.isEmpty()) return;

        for (RechargeOrder order : orders) {
            if (order == null) continue;
            if (!"ALIPAY".equalsIgnoreCase(order.getChannel())) continue;
            try {
                reconcileSingleAlipayOrder(order, now);
            } catch (Exception e) {
                // 避免单笔对账异常导致整批回滚；本任务会在后续调度中再次尝试。
                log.warn("对账补偿失败：orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    private void reconcileSingleAlipayOrder(RechargeOrder order, LocalDateTime now) {
        if (order.getId() == null) return;
        if (!StringUtils.hasText(order.getOrderNo())) return;

        // 避免并发下重复处理
        if ("PAID".equals(order.getStatus())) return;

        AlipayClient.TradeQueryResult qr = alipayClient.queryTrade(order.getOrderNo().trim());
        if (qr == null || qr.tradeStatus == null) return;

        boolean paid = "TRADE_SUCCESS".equalsIgnoreCase(qr.tradeStatus)
                || "TRADE_FINISHED".equalsIgnoreCase(qr.tradeStatus);

        long paidCent = parseYuanToCent(qr.totalAmountYuan);

        if (!paid) {
            // 查询结果非成功：对终态关闭标记 FAILED，其他保持 CLOSED 即可
            RechargeOrder upd = new RechargeOrder();
            upd.setId(order.getId());
            upd.setStatus("TRADE_CLOSED".equalsIgnoreCase(qr.tradeStatus) ? "FAILED" : "CLOSED");
            upd.setCallbackTime(now);
            upd.setCallbackPayload("trade.query:" + qr.tradeStatus);
            upd.setUpdateTime(now);
            rechargeOrderMapper.updateByPrimaryKey(upd);
            return;
        }

        // 金额校验
        if (paidCent != nvl(order.getAmountCent())) {
            throw new BusinessException(ErrorCodeEnum.PAY_AMOUNT_MISMATCH);
        }

        // 更新订单为 PAID（幂等：creditBalance 再次检查流水唯一键）
        RechargeOrder upd = new RechargeOrder();
        upd.setId(order.getId());
        upd.setStatus("PAID");
        upd.setPayTradeNo(qr.tradeNo);
        upd.setPaidTime(now);
        upd.setCallbackTime(now);
        upd.setCallbackPayload("trade.query:" + qr.tradeStatus);
        upd.setUpdateTime(now);
        rechargeOrderMapper.updateByPrimaryKey(upd);

        if (PaymentOrderBizType.MEMBERSHIP.equals(order.getBizType())) {
            membershipService.applyPaymentSuccess(order, now, qr.tradeNo, "ALIPAY");
            return;
        }

        // 入账：钱包 + 流水（幂等）
        creditBalance(order.getUserId(), paidCent, "RECHARGE_ORDER", order.getId(), "recharge-paid:" + order.getOrderNo());

        // 通知：按 biz_ref 去重（防止订单未及时更新导致重复补记）
        long cnt = userNotificationMapper.countByBizRef(order.getUserId(), "RECHARGE_ORDER", order.getId());
        if (cnt <= 0) {
            UserNotification n = new UserNotification();
            n.setUserId(order.getUserId());
            n.setType("RECHARGE_SUCCESS");
            n.setTitle("充值成功");
            n.setContent("充值已到账：" + (paidCent / 100.0) + " 元");
            n.setIsRead(false);
            n.setBizRefType("RECHARGE_ORDER");
            n.setBizRefId(order.getId());
            n.setCreateTime(now);
            n.setUpdateTime(now);
            userNotificationMapper.insert(n);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotify(Map<String, String> params, String rawPayload) {
        if (params == null || params.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        boolean ok = alipayClient.verifyNotify(params);
        if (!ok) {
            throw new BusinessException(ErrorCodeEnum.PAY_CALLBACK_INVALID);
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return true; // 非成功状态也要返回 success，避免平台重试风暴
        }
        String orderNo = params.get("out_trade_no");
        if (!StringUtils.hasText(orderNo)) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "out_trade_no 为空");
        RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo.trim());
        if (order == null) throw new BusinessException(ErrorCodeEnum.RECHARGE_ORDER_NOT_FOUND);

        // 幂等：已入账直接返回
        if ("PAID".equals(order.getStatus())) {
            return true;
        }

        // 金额校验（元 -> 分）
        String totalAmount = params.get("total_amount");
        long paidCent = parseYuanToCent(totalAmount);
        if (paidCent != nvl(order.getAmountCent())) {
            throw new BusinessException(ErrorCodeEnum.PAY_AMOUNT_MISMATCH);
        }

        String tradeNo = params.get("trade_no");
        LocalDateTime now = LocalDateTime.now();

        // 更新订单
        RechargeOrder upd = new RechargeOrder();
        upd.setId(order.getId());
        upd.setStatus("PAID");
        upd.setPayTradeNo(tradeNo);
        upd.setPaidTime(now);
        upd.setCallbackTime(now);
        upd.setCallbackPayload(rawPayload);
        upd.setUpdateTime(now);
        rechargeOrderMapper.updateByPrimaryKey(upd);

        if (PaymentOrderBizType.MEMBERSHIP.equals(order.getBizType())) {
            membershipService.applyPaymentSuccess(order, now, tradeNo, "ALIPAY");
            return true;
        }

        // 入账：钱包 + 流水（幂等键基于 trade_no 或 orderNo）
        creditBalance(order.getUserId(), paidCent, "RECHARGE_ORDER", order.getId(), "recharge-paid:" + orderNo);

        // 通知
        long cnt = userNotificationMapper.countByBizRef(order.getUserId(), "RECHARGE_ORDER", order.getId());
        if (cnt <= 0) {
            UserNotification n = new UserNotification();
            n.setUserId(order.getUserId());
            n.setType("RECHARGE_SUCCESS");
            n.setTitle("充值成功");
            n.setContent("充值已到账：" + (paidCent / 100.0) + " 元");
            n.setIsRead(false);
            n.setBizRefType("RECHARGE_ORDER");
            n.setBizRefId(order.getId());
            n.setCreateTime(now);
            n.setUpdateTime(now);
            userNotificationMapper.insert(n);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWeChatNotify(Map<String, String> params, String rawPayload) {
        if (params == null || params.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        boolean ok = weChatClient.verifyNotify(params);
        if (!ok) {
            throw new BusinessException(ErrorCodeEnum.PAY_CALLBACK_INVALID);
        }

        // v2 回调关键字段：
        // - return_code: SUCCESS/FAIL
        // - result_code: SUCCESS/FAIL
        String returnCode = params.get("return_code");
        String resultCode = params.get("result_code");
        if (!"SUCCESS".equalsIgnoreCase(returnCode) || !"SUCCESS".equalsIgnoreCase(resultCode)) {
            return true; // 非成功状态也返回成功，避免平台重试风暴
        }

        String orderNo = params.get("out_trade_no");
        if (!StringUtils.hasText(orderNo)) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "out_trade_no 为空");

        RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo.trim());
        if (order == null) throw new BusinessException(ErrorCodeEnum.RECHARGE_ORDER_NOT_FOUND);

        // 幂等：已入账直接返回
        if ("PAID".equals(order.getStatus())) {
            return true;
        }

        // 金额校验：total_fee 单位“分”
        String totalFee = params.get("total_fee");
        long paidCent;
        try {
            paidCent = Long.parseLong(totalFee);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.PAY_AMOUNT_MISMATCH, "微信 total_fee 非法");
        }
        if (paidCent != nvl(order.getAmountCent())) {
            throw new BusinessException(ErrorCodeEnum.PAY_AMOUNT_MISMATCH);
        }

        String transactionId = params.get("transaction_id");
        LocalDateTime now = LocalDateTime.now();

        // 更新订单
        RechargeOrder upd = new RechargeOrder();
        upd.setId(order.getId());
        upd.setStatus("PAID");
        upd.setPayTradeNo(transactionId);
        upd.setPaidTime(now);
        upd.setCallbackTime(now);
        upd.setCallbackPayload(rawPayload);
        upd.setUpdateTime(now);
        rechargeOrderMapper.updateByPrimaryKey(upd);

        if (PaymentOrderBizType.MEMBERSHIP.equals(order.getBizType())) {
            membershipService.applyPaymentSuccess(order, now, transactionId, "WECHAT");
            return true;
        }

        // 入账：钱包 + 流水（幂等）
        creditBalance(order.getUserId(), paidCent, "RECHARGE_ORDER", order.getId(), "recharge-paid:" + orderNo);

        // 通知（幂等：按 biz_ref 去重）
        long cnt = userNotificationMapper.countByBizRef(order.getUserId(), "RECHARGE_ORDER", order.getId());
        if (cnt <= 0) {
            UserNotification n = new UserNotification();
            n.setUserId(order.getUserId());
            n.setType("RECHARGE_SUCCESS");
            n.setTitle("充值成功");
            n.setContent("充值已到账：" + (paidCent / 100.0) + " 元");
            n.setIsRead(false);
            n.setBizRefType("RECHARGE_ORDER");
            n.setBizRefId(order.getId());
            n.setCreateTime(now);
            n.setUpdateTime(now);
            userNotificationMapper.insert(n);
        }

        return true;
    }

    private void creditBalance(Long userId, long amountCent, String relatedBizType, Long relatedBizId, String idempotencyKey) {
        WalletLedger existing = walletLedgerMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return;
        }

        UserWallet wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            wallet = ensureWallet(userId);
            wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        }
        long before = nvl(wallet.getTotalBalanceCent());
        long after = before + amountCent;

        UserWallet updWallet = new UserWallet();
        updWallet.setId(wallet.getId());
        updWallet.setTotalBalanceCent(after);
        long frozen = nvl(wallet.getFrozenBalanceCent());
        updWallet.setFrozenBalanceCent(frozen);
        updWallet.setAvailableBalanceCent(after - frozen);
        updWallet.setVersion(nvlI(wallet.getVersion()) + 1);
        updWallet.setResState(1);
        updWallet.setUpdateTime(LocalDateTime.now());
        userWalletMapper.updateByPrimaryKey(updWallet);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setBizType("RECHARGE");
        ledger.setDirection("IN");
        ledger.setAmountCent(amountCent);
        ledger.setBalanceBeforeCent(before);
        ledger.setBalanceAfterCent(after);
        ledger.setRelatedBizType(relatedBizType);
        ledger.setRelatedBizId(relatedBizId);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setRemark(null);
        ledger.setCreateTime(LocalDateTime.now());
        walletLedgerMapper.insert(ledger);
    }

    private UserWallet ensureWallet(Long userId) {
        UserWallet wallet = userWalletMapper.selectByUserId(userId);
        if (wallet != null) return wallet;
        LocalDateTime now = LocalDateTime.now();
        UserWallet w = new UserWallet();
        w.setUserId(userId);
        w.setTotalBalanceCent(0L);
        w.setFrozenBalanceCent(0L);
        w.setAvailableBalanceCent(0L);
        w.setVersion(0);
        w.setResState(1);
        w.setCreateTime(now);
        w.setUpdateTime(now);
        userWalletMapper.insert(w);
        return userWalletMapper.selectByUserId(userId);
    }

    private static long parseYuanToCent(String amountYuan) {
        if (!StringUtils.hasText(amountYuan)) return 0L;
        java.math.BigDecimal bd = new java.math.BigDecimal(amountYuan.trim());
        return bd.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v.longValue();
    }

    private static int nvlI(Integer v) {
        return v == null ? 0 : v.intValue();
    }
}

