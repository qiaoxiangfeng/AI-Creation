package com.aicreation.task;

import com.aicreation.service.AiBillingService;
import com.aicreation.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 计费兜底任务：
 * 当 AI 调用进程异常退出导致预占后未能进入 SETTLED/FAILED，
 * 则将冻结资金解冻并将计费状态置为 FAILED。
 */
@Slf4j
@Component
public class AiBillingAutoReleaseTask {

    @Autowired
    private AiBillingService aiBillingService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Scheduled(cron = "0 */5 * * * ?") // 每 5 分钟执行一次
    public void run() {
        TraceUtil.executeWithTraceId(() -> {
            if (!isRunning.compareAndSet(false, true)) {
                log.info("AI 自动解冻任务正在执行中，跳过本次调度");
                return;
            }
            try {
                aiBillingService.autoReleaseStuckAiBillings(50);
            } catch (Exception e) {
                log.error("AI 自动解冻任务执行失败：{}", e.getMessage(), e);
            } finally {
                isRunning.set(false);
            }
        });
    }
}

