package com.aicreation.constant;

/**
 * 统一支付订单（{@code recharge_order}）业务类型。
 */
public final class PaymentOrderBizType {

    private PaymentOrderBizType() {
    }

    /** 余额充值 */
    public static final String RECHARGE = "RECHARGE";

    /** 会员购买 */
    public static final String MEMBERSHIP = "MEMBERSHIP";
}
