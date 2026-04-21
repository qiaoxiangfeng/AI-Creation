package com.aicreation.external;

import com.aicreation.config.AlipayProperties;
import com.aicreation.exception.BusinessException;
import com.aicreation.enums.ErrorCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支付宝“当面付”最小客户端：预下单（生成二维码）+ 回调验签工具方法。
 *
 * 说明：
 * - 未引入支付宝官方 SDK，采用网关 API + RSA2 自签/验签实现。
 * - 仅覆盖本项目充值场景所需的最小能力。
 */
@Component
public class AlipayClient {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AlipayClient(AlipayProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * 调用 alipay.trade.precreate 生成二维码链接（qr_code）。
     */
    public String precreate(String outTradeNo, long amountCent, String subject) {
        if (props.getAppId() == null || props.getAppId().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 appId 未配置");
        }
        if (props.getPrivateKey() == null || props.getPrivateKey().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 privateKey 未配置");
        }
        if (props.getGateway() == null || props.getGateway().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 gateway 未配置");
        }

        BigDecimal amountYuan = BigDecimal.valueOf(amountCent).movePointLeft(2);
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", outTradeNo);
        biz.put("total_amount", amountYuan.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        biz.put("subject", subject == null ? "余额充值" : subject);

        String bizContent;
        try {
            bizContent = objectMapper.writeValueAsString(biz);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "构造支付宝 biz_content 失败");
        }

        Map<String, String> params = new HashMap<>();
        params.put("app_id", props.getAppId().trim());
        params.put("method", "alipay.trade.precreate");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(TS_FMT));
        params.put("version", "1.0");
        if (props.getNotifyUrl() != null && !props.getNotifyUrl().isBlank()) {
            params.put("notify_url", props.getNotifyUrl().trim());
        }
        params.put("biz_content", bizContent);

        String signContent = buildSignContent(params);
        String sign = Rsa2.sign(signContent, props.getPrivateKey());
        params.put("sign", sign);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k == null || v == null) continue;
            form.add(k, v);
        }

        String gateway = props.getGateway() == null ? "" : props.getGateway().trim();
        String raw = restTemplate.postForObject(java.util.Objects.requireNonNull(gateway), form, String.class);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝网关返回空响应");
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode resp = root.get("alipay_trade_precreate_response");
            if (resp == null || resp.isNull()) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝返回结构异常");
            }
            String code = optText(resp, "code");
            if (!"10000".equals(code)) {
                String msg = optText(resp, "msg");
                String subMsg = optText(resp, "sub_msg");
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝预下单失败: " + msg + (subMsg == null ? "" : (" - " + subMsg)));
            }
            String qrCode = optText(resp, "qr_code");
            if (qrCode == null || qrCode.isBlank()) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝未返回 qr_code");
            }
            return qrCode;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "解析支付宝响应失败: " + e.getMessage());
        }
    }

    /**
     * 调用 alipay.trade.query 查询交易状态。
     *
     * @return TradeQueryResult，若无法解析或网关异常则抛异常。
     */
    public TradeQueryResult queryTrade(String outTradeNo) {
        if (props.getAppId() == null || props.getAppId().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 appId 未配置");
        }
        if (props.getPrivateKey() == null || props.getPrivateKey().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 privateKey 未配置");
        }
        if (props.getGateway() == null || props.getGateway().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝 gateway 未配置");
        }

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", outTradeNo);

        String bizContent;
        try {
            bizContent = objectMapper.writeValueAsString(biz);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "构造支付宝 biz_content 失败");
        }

        Map<String, String> params = new HashMap<>();
        params.put("app_id", props.getAppId().trim());
        params.put("method", "alipay.trade.query");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(TS_FMT));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);

        String signContent = buildSignContent(params);
        String sign = Rsa2.sign(signContent, props.getPrivateKey());
        params.put("sign", sign);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k == null || v == null) continue;
            form.add(k, v);
        }

        String gateway = props.getGateway() == null ? "" : props.getGateway().trim();
        String raw = restTemplate.postForObject(java.util.Objects.requireNonNull(gateway), form, String.class);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝网关返回空响应（trade.query）");
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode resp = root.get("alipay_trade_query_response");
            if (resp == null || resp.isNull()) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝返回结构异常（trade.query）");
            }
            String code = optText(resp, "code");
            if (!"10000".equals(code)) {
                String msg = optText(resp, "msg");
                String subMsg = optText(resp, "sub_msg");
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,
                        "支付宝查询交易失败: " + msg + (subMsg == null ? "" : (" - " + subMsg)));
            }

            String tradeStatus = optText(resp, "trade_status");
            String tradeNo = optText(resp, "trade_no");
            String totalAmount = optText(resp, "total_amount");

            if (!StringUtils.hasText(tradeStatus)) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝查询结果 trade_status 为空");
            }

            return new TradeQueryResult(tradeStatus, tradeNo, totalAmount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "解析支付宝查询响应失败: " + e.getMessage());
        }
    }

    public static class TradeQueryResult {
        public final String tradeStatus;
        public final String tradeNo;
        public final String totalAmountYuan;

        public TradeQueryResult(String tradeStatus, String tradeNo, String totalAmountYuan) {
            this.tradeStatus = tradeStatus;
            this.tradeNo = tradeNo;
            this.totalAmountYuan = totalAmountYuan;
        }
    }

    /**
     * 验签：使用支付宝公钥校验回调参数 sign。
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) return false;
        String sign = params.get("sign");
        String signType = params.getOrDefault("sign_type", "RSA2");
        if (sign == null || sign.isBlank()) return false;
        if (!"RSA2".equalsIgnoreCase(signType)) return false;
        if (props.getAlipayPublicKey() == null || props.getAlipayPublicKey().isBlank()) return false;

        Map<String, String> copy = new HashMap<>(params);
        copy.remove("sign");
        copy.remove("sign_type");
        String signContent = buildSignContent(copy);
        return Rsa2.verify(signContent, sign, props.getAlipayPublicKey());
    }

    private static String optText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /**
     * 按支付宝规则对参数排序并拼接。
     */
    static String buildSignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = params.get(k);
            if (k == null || k.isBlank() || v == null || v.isBlank()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(v);
        }
        return sb.toString();
    }

    /**
     * RSA2 签名/验签工具（PKCS8 私钥 / X509 公钥）。
     */
    static final class Rsa2 {
        private Rsa2() {}

        static String sign(String content, String pkcs8PrivateKey) {
            try {
                java.security.PrivateKey privateKey = KeyLoader.loadPrivateKeyPkcs8(pkcs8PrivateKey);
                java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
                signature.initSign(privateKey);
                signature.update(content.getBytes(StandardCharsets.UTF_8));
                byte[] signed = signature.sign();
                return Base64.getEncoder().encodeToString(signed);
            } catch (Exception e) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "支付宝签名失败: " + e.getMessage());
            }
        }

        static boolean verify(String content, String sign, String alipayPublicKey) {
            try {
                java.security.PublicKey publicKey = KeyLoader.loadPublicKeyX509(alipayPublicKey);
                java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
                signature.initVerify(publicKey);
                signature.update(content.getBytes(StandardCharsets.UTF_8));
                return signature.verify(Base64.getDecoder().decode(sign));
            } catch (Exception e) {
                return false;
            }
        }
    }

    static final class KeyLoader {
        private KeyLoader() {}

        static java.security.PrivateKey loadPrivateKeyPkcs8(String key) throws Exception {
            String pem = stripPem(key);
            byte[] bytes = Base64.getDecoder().decode(pem);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(bytes);
            return java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);
        }

        static java.security.PublicKey loadPublicKeyX509(String key) throws Exception {
            String pem = stripPem(key);
            byte[] bytes = Base64.getDecoder().decode(pem);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(bytes);
            return java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
        }

        static String stripPem(String s) {
            if (s == null) return "";
            return s
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
        }
    }
}

