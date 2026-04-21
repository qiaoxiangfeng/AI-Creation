package com.aicreation.mapper;

import com.aicreation.entity.po.AiUsageBilling;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiUsageBillingMapper {

    AiUsageBilling selectByPrimaryKey(@Param("id") Long id);

    AiUsageBilling selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(AiUsageBilling record);

    int updateByPrimaryKey(AiUsageBilling record);

    List<AiUsageBilling> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询长时间未结算/未解冻的计费记录（预占/成功态停留）。
     */
    List<AiUsageBilling> selectStuckAiBillings(
            @Param("beforeTime") java.time.LocalDateTime beforeTime,
            @Param("limit") int limit
    );
}

