package com.aicreation.service.impl;

import com.aicreation.entity.po.AiUsageBilling;
import com.aicreation.entity.po.UserNotification;
import com.aicreation.entity.po.UserWallet;
import com.aicreation.entity.po.WalletLedger;
import com.aicreation.enums.ErrorCodeEnum;
import com.aicreation.exception.BusinessException;
import com.aicreation.mapper.AiUsageBillingMapper;
import com.aicreation.mapper.UserNotificationMapper;
import com.aicreation.mapper.UserWalletMapper;
import com.aicreation.mapper.WalletLedgerMapper;
import com.aicreation.service.AiBillingService;
import com.aicreation.service.MembershipService;
import com.aicreation.util.TraceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiBillingServiceImpl implements AiBillingService {

    @Autowired
    private UserWalletMapper userWalletMapper;

    @Autowired
    private WalletLedgerMapper walletLedgerMapper;

    @Autowired
    private AiUsageBillingMapper aiUsageBillingMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Autowired
    private MembershipService membershipService;

    /**
     * 自我注入代理，用于让 @Transactional 在内部调用时生效。
     */
    @Autowired
    @Lazy
    private AiBillingServiceImpl self;

    @Override
    public <T> T executeWithAiBilling(
            Long userId,
            String bizScene,
            Long articleId,
            Long chapterId,
            long estimatedCostCent,
            Supplier<T> aiCall
    ) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_REQUIRED);
        }
        membershipService.assertActiveMembershipForAi(userId);
        if (bizScene == null || bizScene.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "bizScene不能为空");
        }
        if (aiCall == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "aiCall不能为空");
        }

        long preAuthCent = Math.max(1L, estimatedCostCent);
        String traceId = TraceUtil.getTraceId();
        // traceId 由拦截器生成；在任务场景下也会由 TraceUtil.executeWithTraceId 注入
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceUtil.generateTraceId();
        }

        String idempotencyKey = buildAiIdempotencyKey(userId, bizScene, articleId, chapterId, traceId);

        // 1) 预占与写入计费/流水（短事务）
        Long aiUsageId = self.preAuth(userId, bizScene, articleId, chapterId, preAuthCent, idempotencyKey, traceId);

        // 2) 执行 AI（长耗时，无锁）
        try {
            T result = aiCall.get();

            // 2.1) 记录 AI 调用成功（用于后续结算兜底判断）
            self.markAiCallSuccess(aiUsageId);

            // 3) 结算扣费（短事务）
            self.settle(aiUsageId, preAuthCent, null);
            return result;
        } catch (Exception e) {
            // 4) 失败解冻（短事务）
            String errMsg = e.getMessage();
            if (errMsg == null) errMsg = e.getClass().getSimpleName();
            self.release(aiUsageId, preAuthCent, errMsg);

            // 不吞异常，让上层直接返回业务码（如余额不足）
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw e;
        }
    }

    @Override
    public long estimateCostCent(String bizScene, Integer lengthEstimate) {
        // lengthEstimate 用于大致估算（字数/提示词长度），目前无法拿到真实 token 计费数据。
        long len = lengthEstimate == null ? 0L : lengthEstimate.longValue();
        if (len <= 0) len = 2000L;

        // “分”级别的估算参数
        long costCent;
        if ("GENERATE_SINGLE_TITLE".equals(bizScene)) {
            // 单篇标题+大纲（配置页「生成标题」）
            costCent = Math.max(15L, len / 250L);
        } else if ("GENERATE_CHAPTERS".equals(bizScene)) {
            // 章节基础信息（标题/梗概/完结判断）
            costCent = Math.max(10L, len / 200L);
        } else if ("GENERATE_CHAPTER_CONTENT".equals(bizScene) || "REGENERATE_CHAPTER_CONTENT".equals(bizScene)) {
            // 正文生成 / 重新生成正文
            costCent = Math.max(50L, len / 100L);
        } else if ("REFINE_OUTLINE".equals(bizScene)) {
            // 修订大纲
            costCent = Math.max(30L, len / 150L);
        } else {
            // 默认兜底：避免估算为0导致绕过扣费
            costCent = Math.max(20L, len / 200L);
        }

        return costCent;
    }

    private String buildAiIdempotencyKey(
            Long userId,
            String bizScene,
            Long articleId,
            Long chapterId,
            String traceId
    ) {
        // traceId 粒度足够保证链路唯一；用于防重复预占/结算。
        return "ai-billing:" + userId + ":" + bizScene + ":" + nvl(articleId) + ":" + nvl(chapterId) + ":" + traceId;
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private static final String PROVIDER = "VOLCENGINE";

    @Transactional(rollbackFor = Exception.class)
    public Long preAuth(
            Long userId,
            String bizScene,
            Long articleId,
            Long chapterId,
            long preAuthCent,
            String idempotencyKey,
            String traceId
    ) {
        if (preAuthCent <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "estimatedCostCent必须>0");
        }

        // 幂等：已存在同一预占记录则复用
        AiUsageBilling existing = aiUsageBillingMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            if ("PRE_AUTHED".equals(existing.getStatus())) {
                return existing.getId();
            }
            // 若已结算/失败，直接允许上层重新执行（以新的 traceId 视为新请求）
            if (existing.getId() != null) {
                return existing.getId();
            }
        }

        UserWallet wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            wallet = ensureWalletForUpdate(userId);
        }

        Long availableBefore = wallet.getAvailableBalanceCent();
        if (availableBefore == null) availableBefore = 0L;

        if (availableBefore < preAuthCent) {
            notifyBalanceInsufficient(userId, availableBefore, preAuthCent);
            throw new BusinessException(ErrorCodeEnum.BALANCE_INSUFFICIENT);
        }

        long frozenBefore = wallet.getFrozenBalanceCent() == null ? 0L : wallet.getFrozenBalanceCent();
        long totalBefore = wallet.getTotalBalanceCent() == null ? 0L : wallet.getTotalBalanceCent();

        long frozenAfter = frozenBefore + preAuthCent;
        long totalAfter = totalBefore; // 预占阶段不改变 total
        long availableAfter = totalAfter - frozenAfter;

        // 1) 更新钱包冻结金额
        UserWallet updWallet = new UserWallet();
        updWallet.setId(wallet.getId());
        updWallet.setFrozenBalanceCent(frozenAfter);
        updWallet.setAvailableBalanceCent(availableAfter);
        updWallet.setVersion(Objects.requireNonNullElse(wallet.getVersion(), 0) + 1);
        updWallet.setResState(wallet.getResState() == null ? 1 : wallet.getResState());
        updWallet.setUpdateTime(LocalDateTime.now());
        userWalletMapper.updateByPrimaryKey(updWallet);

        // 2) 插入计费预占记录
        LocalDateTime now = LocalDateTime.now();
        AiUsageBilling billing = new AiUsageBilling();
        billing.setUserId(userId);
        billing.setArticleId(articleId);
        billing.setChapterId(chapterId);
        billing.setBizScene(bizScene);
        billing.setProvider(PROVIDER);
        billing.setModelName(null);
        billing.setRequestTokens(0);
        billing.setResponseTokens(0);
        billing.setTotalTokens(0);
        billing.setEstimatedCostCent(preAuthCent);
        billing.setActualCostCent(0L);
        billing.setPreAuthAmountCent(preAuthCent);
        billing.setSettledAmountCent(0L);
        billing.setRefundAmountCent(0L);
        billing.setStatus("PRE_AUTHED");
        billing.setTraceId(traceId);
        billing.setIdempotencyKey(idempotencyKey);
        billing.setErrorMessage(null);
        billing.setCreateTime(now);
        billing.setUpdateTime(now);
        aiUsageBillingMapper.insert(billing);

        // 3) 记录钱包流水（冻结）
        String ledgerIdem = idempotencyKey + ":FREEZE";
        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setBizType("AI_PRE_AUTH");
        ledger.setDirection("FREEZE");
        ledger.setAmountCent(preAuthCent);
        ledger.setBalanceBeforeCent(availableBefore);
        ledger.setBalanceAfterCent(availableAfter);
        ledger.setRelatedBizType("AI_USAGE");
        ledger.setRelatedBizId(billing.getId());
        ledger.setIdempotencyKey(ledgerIdem);
        ledger.setRemark("预占AI费用");
        ledger.setCreateTime(now);
        walletLedgerMapper.insert(ledger);

        return billing.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void settle(Long aiUsageId, long actualCostCent, String errorMessage) {
        AiUsageBilling billing = aiUsageBillingMapper.selectByPrimaryKey(aiUsageId);
        if (billing == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        if (!"PRE_AUTHED".equals(billing.getStatus()) && !"SUCCESS".equals(billing.getStatus())) {
            // 已结算/失败/取消等状态直接返回，避免重复扣费
            return;
        }

        long preAuthCent = billing.getPreAuthAmountCent() == null ? 0L : billing.getPreAuthAmountCent();
        if (actualCostCent <= 0) actualCostCent = preAuthCent;

        Long userId = billing.getUserId();

        UserWallet wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        long beforeAvailable = wallet.getAvailableBalanceCent() == null ? 0L : wallet.getAvailableBalanceCent();
        long beforeTotal = wallet.getTotalBalanceCent() == null ? 0L : wallet.getTotalBalanceCent();
        long beforeFrozen = wallet.getFrozenBalanceCent() == null ? 0L : wallet.getFrozenBalanceCent();

        long totalAfter = beforeTotal - actualCostCent;
        long frozenAfter = beforeFrozen - preAuthCent;
        if (frozenAfter < 0) frozenAfter = 0;
        long availableAfter = totalAfter - frozenAfter;

        // 更新计费状态
        LocalDateTime now = LocalDateTime.now();
        AiUsageBilling upd = new AiUsageBilling();
        upd.setId(billing.getId());
        upd.setStatus("SETTLED");
        upd.setActualCostCent(actualCostCent);
        upd.setSettledAmountCent(actualCostCent);
        upd.setRefundAmountCent(0L);
        upd.setErrorMessage(null);
        upd.setUpdateTime(now);
        aiUsageBillingMapper.updateByPrimaryKey(upd);

        // 更新钱包余额
        UserWallet updWallet = new UserWallet();
        updWallet.setId(wallet.getId());
        updWallet.setTotalBalanceCent(totalAfter);
        updWallet.setFrozenBalanceCent(frozenAfter);
        updWallet.setAvailableBalanceCent(availableAfter);
        updWallet.setVersion(Objects.requireNonNullElse(wallet.getVersion(), 0) + 1);
        updWallet.setUpdateTime(now);
        updWallet.setResState(wallet.getResState() == null ? 1 : wallet.getResState());
        userWalletMapper.updateByPrimaryKey(updWallet);

        // 钱包流水（扣款）
        String ledgerIdem = billing.getIdempotencyKey() + ":SETTLE_DEBIT";
        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setBizType("AI_SETTLE_DEBIT");
        ledger.setDirection("OUT");
        ledger.setAmountCent(actualCostCent);
        ledger.setBalanceBeforeCent(beforeAvailable);
        ledger.setBalanceAfterCent(availableAfter);
        ledger.setRelatedBizType("AI_USAGE");
        ledger.setRelatedBizId(billing.getId());
        ledger.setIdempotencyKey(ledgerIdem);
        ledger.setRemark(null);
        ledger.setCreateTime(now);
        walletLedgerMapper.insert(ledger);

        // 通知
        createAiDeductionNotification(userId, availableAfter, actualCostCent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void release(Long aiUsageId, long refundCent, String errorMessage) {
        AiUsageBilling billing = aiUsageBillingMapper.selectByPrimaryKey(aiUsageId);
        if (billing == null) {
            return;
        }
        if (!"PRE_AUTHED".equals(billing.getStatus()) && !"SUCCESS".equals(billing.getStatus())) {
            // 已结算/失败等状态无需重复解冻
            return;
        }

        Long userId = billing.getUserId();

        UserWallet wallet = userWalletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            return;
        }

        long beforeAvailable = wallet.getAvailableBalanceCent() == null ? 0L : wallet.getAvailableBalanceCent();
        long beforeFrozen = wallet.getFrozenBalanceCent() == null ? 0L : wallet.getFrozenBalanceCent();
        long totalBefore = wallet.getTotalBalanceCent() == null ? 0L : wallet.getTotalBalanceCent();

        long frozenAfter = beforeFrozen - refundCent;
        if (frozenAfter < 0) frozenAfter = 0;
        long availableAfter = totalBefore - frozenAfter;

        LocalDateTime now = LocalDateTime.now();

        // 更新计费状态
        AiUsageBilling upd = new AiUsageBilling();
        upd.setId(billing.getId());
        upd.setStatus("FAILED");
        upd.setActualCostCent(0L);
        upd.setSettledAmountCent(0L);
        upd.setRefundAmountCent(refundCent);
        upd.setErrorMessage(errorMessage);
        upd.setUpdateTime(now);
        aiUsageBillingMapper.updateByPrimaryKey(upd);

        // 更新钱包解冻
        UserWallet updWallet = new UserWallet();
        updWallet.setId(wallet.getId());
        updWallet.setFrozenBalanceCent(frozenAfter);
        updWallet.setAvailableBalanceCent(availableAfter);
        updWallet.setVersion(Objects.requireNonNullElse(wallet.getVersion(), 0) + 1);
        updWallet.setUpdateTime(now);
        updWallet.setResState(wallet.getResState() == null ? 1 : wallet.getResState());
        userWalletMapper.updateByPrimaryKey(updWallet);

        // 钱包流水（解冻）
        String ledgerIdem = billing.getIdempotencyKey() + ":UNFREEZE";
        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setBizType("AI_SETTLE_REFUND");
        ledger.setDirection("UNFREEZE");
        ledger.setAmountCent(refundCent);
        ledger.setBalanceBeforeCent(beforeAvailable);
        ledger.setBalanceAfterCent(availableAfter);
        ledger.setRelatedBizType("AI_USAGE");
        ledger.setRelatedBizId(billing.getId());
        ledger.setIdempotencyKey(ledgerIdem);
        ledger.setRemark(errorMessage);
        ledger.setCreateTime(now);
        walletLedgerMapper.insert(ledger);
    }

    private void notifyBalanceInsufficient(Long userId, Long availableCent, long neededCent) {
        try {
            LocalDateTime now = LocalDateTime.now();
            UserNotification n = new UserNotification();
            n.setUserId(userId);
            n.setType("BALANCE_LOW");
            n.setTitle("余额不足");
            double avail = availableCent == null ? 0D : (availableCent / 100.0);
            double need = neededCent / 100.0;
            n.setContent("可用余额不足（可用：" + avail + " 元，预计需要：" + need + " 元），请先充值后再使用 AI。");
            n.setIsRead(false);
            n.setBizRefType("WALLET");
            n.setBizRefId(null);
            n.setCreateTime(now);
            n.setUpdateTime(now);
            userNotificationMapper.insert(n);
        } catch (Exception ignore) {
            // 通知失败不影响扣费门禁流程
        }
    }

    private void createAiDeductionNotification(Long userId, long availableAfterCent, long deductedCent) {
        try {
            LocalDateTime now = LocalDateTime.now();
            UserNotification n = new UserNotification();
            n.setUserId(userId);
            n.setType("AI_CHARGE");
            n.setTitle("AI扣费成功");
            n.setContent("本次消耗约 " + (deductedCent / 100.0) + " 元，当前可用余额：" + (availableAfterCent / 100.0) + " 元。");
            n.setIsRead(false);
            n.setBizRefType("AI_USAGE");
            n.setBizRefId(null);
            n.setCreateTime(now);
            n.setUpdateTime(now);
            userNotificationMapper.insert(n);
        } catch (Exception ignore) {
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAiCallSuccess(Long aiUsageId) {
        AiUsageBilling billing = aiUsageBillingMapper.selectByPrimaryKey(aiUsageId);
        if (billing == null) return;
        if (!"PRE_AUTHED".equals(billing.getStatus()) && !"SUCCESS".equals(billing.getStatus())) {
            return;
        }
        AiUsageBilling upd = new AiUsageBilling();
        upd.setId(aiUsageId);
        upd.setStatus("SUCCESS");
        upd.setUpdateTime(LocalDateTime.now());
        aiUsageBillingMapper.updateByPrimaryKey(upd);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserWallet ensureWalletForUpdate(Long userId) {
        // 仅在 wallet 为空时创建
        UserWallet existing = userWalletMapper.selectByUserId(userId);
        if (existing != null) return existing;

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
        return userWalletMapper.selectByUserIdForUpdate(userId);
    }

    @Override
    public void autoReleaseStuckAiBillings(int limit) {
        if (limit <= 0) return;

        // 兜底：预占后超过 10 分钟仍未结算/解冻，则认为链路失败，进行自动解冻。
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(10);
        java.util.List<com.aicreation.entity.po.AiUsageBilling> stuck =
                aiUsageBillingMapper.selectStuckAiBillings(beforeTime, limit);

        if (stuck == null || stuck.isEmpty()) return;

        for (com.aicreation.entity.po.AiUsageBilling billing : stuck) {
            if (billing == null || billing.getId() == null) continue;
            long refundCent = billing.getPreAuthAmountCent() == null ? 0L : billing.getPreAuthAmountCent();
            if (refundCent <= 0) continue;

            try {
                self.release(billing.getId(), refundCent, "AUTO_RELEASE_TIMEOUT");
            } catch (Exception e) {
                // 单条失败不影响批次；下一轮定时任务会重试
                log.warn("自动解冻失败：aiUsageId={}, error={}", billing.getId(), e.getMessage());
            }
        }
    }
}

