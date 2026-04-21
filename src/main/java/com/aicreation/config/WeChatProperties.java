package com.aicreation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置（Native 扫码：统一下单）
 *
 * 说明：
 * - 当前实现采用 WeChat Pay v2 的统一下单（unifiedorder）+ MD5 验签。
 * - 后续如升级到 v3，需要证书/平台 API 变更。
 */
@Component
@ConfigurationProperties(prefix = "app.wechat")
public class WeChatProperties {

    private String appId;
    private String mchId;
    private String apiKey;

    /**
     * 微信回调地址（notify_url）
     */
    private String notifyUrl;

    /**
     * 发起回调用的客户端 IP（spbill_create_ip）
     */
    private String spbillCreateIp = "127.0.0.1";

    /**
     * unifiedorder 接口地址
     */
    private String unifiedOrderUrl = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getSpbillCreateIp() {
        return spbillCreateIp;
    }

    public void setSpbillCreateIp(String spbillCreateIp) {
        this.spbillCreateIp = spbillCreateIp;
    }

    public String getUnifiedOrderUrl() {
        return unifiedOrderUrl;
    }

    public void setUnifiedOrderUrl(String unifiedOrderUrl) {
        this.unifiedOrderUrl = unifiedOrderUrl;
    }
}

