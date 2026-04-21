package com.aicreation.mapper;

import com.aicreation.entity.po.MembershipSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MembershipSubscriptionMapper {

    int insert(MembershipSubscription row);

    MembershipSubscription selectByPaymentOrderId(@Param("paymentOrderId") Long paymentOrderId);
}
