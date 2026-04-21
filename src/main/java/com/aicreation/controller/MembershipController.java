package com.aicreation.controller;

import com.aicreation.entity.dto.MembershipCreateOrderReqDto;
import com.aicreation.entity.dto.MembershipPricingItemRespDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员", description = "会员定价与购买")
@RestController
@RequestMapping("/membership")
public class MembershipController {

    @Autowired
    private MembershipService membershipService;

    @Operation(summary = "可售会员档位", description = "含计算后的应付金额（分）")
    @GetMapping("/pricing")
    public BaseResponse<List<MembershipPricingItemRespDto>> pricing() {
        return BaseResponse.success(membershipService.listEnabledPricing());
    }

    @Operation(summary = "创建会员支付订单", description = "返回支付宝/微信预下单内容")
    @PostMapping("/order/create")
    public BaseResponse<RechargeCreateRespDto> createOrder(@Valid @RequestBody MembershipCreateOrderReqDto req) {
        return BaseResponse.success(membershipService.createMembershipOrder(req));
    }
}
