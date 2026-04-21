package com.aicreation.controller;

import com.aicreation.entity.dto.RechargeCreateReqDto;
import com.aicreation.entity.dto.RechargeCreateRespDto;
import com.aicreation.entity.dto.WalletBalanceRespDto;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.po.RechargeOrder;
import com.aicreation.entity.po.WalletLedger;
import com.aicreation.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Tag(name = "钱包与充值", description = "钱包余额、流水、充值订单与回调")
@RestController
@RequestMapping("/wallet")
@Slf4j
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Operation(summary = "查询余额", description = "查询当前登录用户余额")
    @GetMapping("/balance")
    public BaseResponse<WalletBalanceRespDto> balance() {
        return BaseResponse.success(walletService.getCurrentUserBalance());
    }

    @Operation(summary = "流水列表", description = "查询当前用户钱包流水（倒序）")
    @PostMapping("/ledger/list")
    public BaseResponse<List<WalletLedger>> ledgerList() {
        return BaseResponse.success(walletService.listCurrentUserLedger());
    }

    @Operation(summary = "创建充值订单", description = "创建支付宝当面付订单并返回二维码内容")
    @PostMapping("/recharge/create")
    public BaseResponse<RechargeCreateRespDto> createRecharge(@Valid @RequestBody RechargeCreateReqDto req) {
        return BaseResponse.success(walletService.createRechargeOrder(req));
    }

    @Operation(summary = "查询充值订单", description = "查询订单状态")
    @GetMapping("/recharge/{orderNo}")
    public BaseResponse<RechargeOrder> getRecharge(@Parameter(description = "订单号") @PathVariable String orderNo) {
        return BaseResponse.success(walletService.getRechargeOrder(orderNo));
    }

    /**
     * 支付宝异步通知回调：必须返回 "success" / "failure" 文本。
     */
    @Operation(summary = "支付宝回调", description = "支付宝异步通知（无需登录）")
    @PostMapping("/recharge/callback/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        StringBuilder raw = new StringBuilder();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getParameter(name);
            if (value == null) value = "";
            params.put(name, value);
            if (raw.length() > 0) raw.append('&');
            raw.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        try {
            walletService.handleAlipayNotify(params, raw.toString());
            return "success";
        } catch (Exception e) {
            log.error("支付宝异步通知处理失败: {}", e.getMessage(), e);
            return "failure";
        }
    }

    /**
     * 微信支付异步通知回调：必须返回 SUCCESS/FAIL XML。
     */
    @Operation(summary = "微信回调", description = "微信支付异步通知（无需登录）")
    @PostMapping("/recharge/callback/wechat")
    public String wechatNotify(HttpServletRequest request) {
        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) return wechatFailXml();
            Map<String, String> params = parseXmlToMap(body);
            walletService.handleWeChatNotify(params, body);
            return wechatSuccessXml();
        } catch (Exception e) {
            log.error("微信异步通知处理失败: {}", e.getMessage(), e);
            return wechatFailXml();
        }
    }

    private String wechatSuccessXml() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    private String wechatFailXml() {
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[ERROR]]></return_msg></xml>";
    }

    private Map<String, String> parseXmlToMap(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("*");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            String name = nodeList.item(i).getNodeName();
            String value = nodeList.item(i).getTextContent();
            if (value == null) value = "";
            if ("#text".equals(name)) continue;
            // 跳过 XML 根节点，避免把 xml 本身参与验签
            if ("xml".equalsIgnoreCase(name)) continue;
            if (map.containsKey(name)) continue;
            map.put(name, value.trim());
        }
        return map;
    }
}

