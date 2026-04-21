package com.aicreation.controller;

import com.aicreation.entity.dto.AdminMembershipGrantMonthsReqDto;
import com.aicreation.entity.dto.AdminMembershipRefundRollbackReqDto;
import com.aicreation.entity.dto.AdminMembershipSetEndReqDto;
import com.aicreation.entity.dto.MembershipPricingConfigSaveReqDto;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.po.MembershipPricingConfig;
import com.aicreation.security.AccessControlService;
import com.aicreation.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员管理（管理员）", description = "定价配置与人工调整")
@RestController
@RequestMapping("/admin/membership")
public class MembershipAdminController {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private AccessControlService accessControlService;

    @Operation(summary = "全部定价配置")
    @GetMapping("/pricing/list")
    public BaseResponse<List<MembershipPricingConfig>> listPricing() {
        accessControlService.assertAdmin();
        return BaseResponse.success(membershipService.listAllPricingForAdmin());
    }

    @Operation(summary = "保存定价配置", description = "id 为空则新增")
    @PostMapping("/pricing/save")
    public BaseResponse<Boolean> savePricing(@Valid @RequestBody MembershipPricingConfigSaveReqDto req) {
        accessControlService.assertAdmin();
        membershipService.savePricingConfig(req);
        return BaseResponse.success(true);
    }

    @Operation(summary = "管理员赠送/顺延会员（按月）")
    @PostMapping("/grant-months")
    public BaseResponse<Boolean> grantMonths(@Valid @RequestBody AdminMembershipGrantMonthsReqDto req) {
        accessControlService.assertAdmin();
        membershipService.adminGrantOrExtendMonths(
                req.getTargetUserId(),
                req.getMonths(),
                req.getRemark()
        );
        return BaseResponse.success(true);
    }

    @Operation(summary = "管理员指定会员结束时间")
    @PostMapping("/set-end")
    public BaseResponse<Boolean> setEnd(@Valid @RequestBody AdminMembershipSetEndReqDto req) {
        accessControlService.assertAdmin();
        membershipService.adminSetEndTime(req.getTargetUserId(), req.getNewEndAt(), req.getRemark());
        return BaseResponse.success(true);
    }

    @Operation(summary = "退款后回滚会员结束时间", description = "仅当当前结束时间与该支付单快照一致时生效")
    @PostMapping("/refund-rollback")
    public BaseResponse<Boolean> refundRollback(@Valid @RequestBody AdminMembershipRefundRollbackReqDto req) {
        accessControlService.assertAdmin();
        boolean ok = membershipService.rollbackMembershipForPaymentOrder(
                req.getPaymentOrderId(),
                req.getRemark()
        );
        return BaseResponse.success(ok);
    }
}
