package com.aicreation.external;

import com.aicreation.config.WeChatProperties;
import com.aicreation.exception.BusinessException;
import com.aicreation.enums.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 微信支付最小客户端（Native 扫码：统一下单 + 回调验签）
 *
 * 说明：
 * - 采用 WeChat Pay v2 unifiedorder（MD5 签名）
 * - 未引入微信官方 SDK，按回调/下单字段最小落地
 */
@Slf4j
@Component
public class WeChatClient {

    private final WeChatProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public WeChatClient(WeChatProperties props) {
        this.props = props;
    }

    public String createNativePay(String outTradeNo, long amountCent, String subject) {
        validateConfig();

        long totalFee = amountCent; // 统一下单 total_fee 需要“分”
        if (totalFee <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "amountCent必须>0");
        }

        // 显式 requireNonNull：消除静态分析的 nullness 警告
        String appId = Objects.requireNonNull(props.getAppId(), "微信 appId 未配置").trim();
        String mchId = Objects.requireNonNull(props.getMchId(), "微信 mchId 未配置").trim();
        String apiKey = Objects.requireNonNull(props.getApiKey(), "微信 apiKey 未配置");
        String notifyUrl = Objects.requireNonNull(props.getNotifyUrl(), "微信 notifyUrl 未配置").trim();
        String spbillCreateIp = Objects.requireNonNull(props.getSpbillCreateIp(), "微信 spbill_create_ip 未配置").trim();
        String unifiedOrderUrl = Objects.requireNonNull(props.getUnifiedOrderUrl(), "微信 unifiedOrderUrl 未配置").trim();

        String nonceStr = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("appid", appId);
        biz.put("mch_id", mchId);
        biz.put("nonce_str", nonceStr);
        biz.put("body", subject == null ? "余额充值" : subject);
        biz.put("out_trade_no", outTradeNo);
        biz.put("total_fee", String.valueOf(totalFee));
        biz.put("spbill_create_ip", spbillCreateIp);
        biz.put("notify_url", notifyUrl);
        biz.put("trade_type", "NATIVE");

        // v2 unifiedorder：sign = md5(拼接 + &key=API_KEY)
        String sign = signByMd5(biz, apiKey);
        biz.put("sign", sign);

        String reqXml = toXml(biz);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> entity = new HttpEntity<>(reqXml, headers);

        String raw = restTemplate.postForObject(java.util.Objects.requireNonNull(unifiedOrderUrl), entity, String.class);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 unifiedorder 响应为空");
        }

        Map<String, String> resp = fromXmlToMap(raw);
        String returnCode = nvl(resp.get("return_code"));
        if (!"SUCCESS".equalsIgnoreCase(returnCode)) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,
                    "微信 unifiedorder 失败: return_code=" + returnCode + ", msg=" + resp.get("return_msg"));
        }
        String resultCode = nvl(resp.get("result_code"));
        if (!"SUCCESS".equalsIgnoreCase(resultCode)) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,
                    "微信 unifiedorder 失败: result_code=" + resultCode + ", err=" + resp.get("err_code_des"));
        }

        String codeUrl = resp.get("code_url");
        if (codeUrl == null || codeUrl.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信未返回 code_url");
        }
        return codeUrl;
    }

    /**
     * 验签（回调参数中包含 sign）
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) return false;

        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) return false;

        Map<String, String> signParams = new HashMap<>(params);
        signParams.remove("sign");
        // 仅排除 sign；如回调携带 sign_type（通常为 MD5），也参与签名计算以保持兼容

        String apiKey = Objects.requireNonNull(props.getApiKey(), "微信 apiKey 未配置");
        String expected = signByMd5(signParams, apiKey);
        return expected.equalsIgnoreCase(sign.trim());
    }

    private void validateConfig() {
        if (props.getAppId() == null || props.getAppId().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 appId 未配置");
        }
        if (props.getMchId() == null || props.getMchId().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 mchId 未配置");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 apiKey 未配置");
        }
        if (props.getNotifyUrl() == null || props.getNotifyUrl().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 notifyUrl 未配置");
        }
        if (props.getUnifiedOrderUrl() == null || props.getUnifiedOrderUrl().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 unifiedOrderUrl 未配置");
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * WeChat v2 MD5 sign
     */
    private static String signByMd5(Map<String, String> params, String apiKey) {
        if (params == null) return "";
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (k == null) continue;
            String v = params.get(k);
            if (v == null || v.isBlank()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(v);
        }
        sb.append("&key=").append(apiKey);

        return md5Upper(sb.toString());
    }

    private static String md5Upper(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff).toUpperCase();
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "MD5 签名失败: " + e.getMessage());
        }
    }

    private static String toXml(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() == null) continue;
            String v = e.getValue() == null ? "" : e.getValue();
            sb.append("<").append(e.getKey()).append(">");
            sb.append("<![CDATA[").append(v).append("]]>");
            sb.append("</").append(e.getKey()).append(">");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    private static Map<String, String> fromXmlToMap(String xml) {
        try {
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
                // unifiedorder 响应中会有 xml 根节点，我们也可以保留
                if (map.containsKey(name)) continue;
                map.put(name, value.trim());
            }
            return map;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "微信 XML 解析失败: " + e.getMessage());
        }
    }
}

