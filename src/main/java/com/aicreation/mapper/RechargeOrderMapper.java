package com.aicreation.mapper;

import com.aicreation.entity.po.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RechargeOrderMapper {

    RechargeOrder selectByPrimaryKey(@Param("id") Long id);

    RechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    RechargeOrder selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(RechargeOrder order);

    int updateByPrimaryKey(RechargeOrder order);

    List<RechargeOrder> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询已过期且未入账（status != PAID）的订单，用于超时关闭/对账补偿。
     */
    List<RechargeOrder> selectExpiredUnpaidOrders(
            @Param("beforeTime") java.time.LocalDateTime beforeTime,
            @Param("limit") int limit
    );

    /**
     * 将已过期未入账的订单置为 CLOSED（不做入账）。
     */
    int markExpiredUnpaidOrdersAsClosed(@Param("now") java.time.LocalDateTime now);
}

