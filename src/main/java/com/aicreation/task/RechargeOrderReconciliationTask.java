package com.aicreation.task;

import com.aicreation.service.WalletService;
import com.aicreation.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 充值订单超时关闭 + 支付对账补偿任务
 */
@Slf4j
@Component
public class RechargeOrderReconciliationTask {

    @Autowired
    private WalletService walletService;

    /**
     * 避免定时任务并发叠加
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 每分钟执行一次兜底对账补偿
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeAndReconcileExpiredRechargeOrders() {
        TraceUtil.executeWithTraceId(() -> {
            if (!isRunning.compareAndSet(false, true)) {
                log.info("充值对账任务正在执行中，跳过本次调度");
                return;
            }
            try {
                // 每次最多处理少量，避免长事务/长查询影响主流程
                walletService.closeAndReconcileExpiredRechargeOrders(50);
            } catch (Exception e) {
                log.error("充值对账任务执行失败：{}", e.getMessage(), e);
            } finally {
                isRunning.set(false);
            }
        });
    }
}

