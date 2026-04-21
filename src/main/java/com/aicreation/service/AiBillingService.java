package com.aicreation.service;

import java.util.function.Supplier;

/**
 * AI 使用计费与扣费门禁服务。
 *
 * 计费策略：
 * - 扣费使用“预占 -> 执行 AI -> 结算 / 失败解冻”的两段式流程。
 * - 本项目目前没有从火山引擎返回 token 精确数据，因此实际扣费金额暂按预估值结算（并会在表中记录）。
 */
public interface AiBillingService {

    /**
     * 在执行 AI 调用前进行余额预占；执行成功后结算；执行失败则解冻。
     *
     * @param userId 当前用户ID
     * @param bizScene 计费业务场景（如：GENERATE_CHAPTERS/GENERATE_CHAPTER_CONTENT/REGENERATE_CHAPTER_CONTENT/REFINE_OUTLINE）
     * @param articleId 文章ID（可空）
     * @param chapterId 章节ID（可空）
     * @param estimatedCostCent 预估成本（分）
     * @param aiCall AI 调用逻辑
     */
    <T> T executeWithAiBilling(
            Long userId,
            String bizScene,
            Long articleId,
            Long chapterId,
            long estimatedCostCent,
            Supplier<T> aiCall
    );

    /**
     * 根据业务场景和“字数/长度预估”计算预估成本（分）。
     * <p>
     * 注意：此估算用于扣费门禁与记录，不代表真实 token 计费。
     */
    long estimateCostCent(String bizScene, Integer lengthEstimate);

    /**
     * 定时任务：当 AI 调用进程异常退出导致预占后未能结算/解冻时，
     * 兜底将冻结资金解冻并将计费状态置为 FAILED。
     */
    void autoReleaseStuckAiBillings(int limit);
}

