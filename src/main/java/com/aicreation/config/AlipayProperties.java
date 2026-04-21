package com.aicreation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置（当面付扫码）
 */
@Component
@ConfigurationProperties(prefix = "app.alipay")
public class AlipayProperties {

    /**
     * 支付宝开放平台 APPID
     */
    private String appId;

    /**
     * 应用私钥（PKCS8，RSA2）
     */
    private String privateKey;

    /**
     * 支付宝公钥（RSA2）
     */
    private String alipayPublicKey;

    /**
     * 网关地址（生产：https://openapi.alipay.com/gateway.do；沙箱：https://openapi.alipaydev.com/gateway.do）
     */
    private String gateway = "https://openapi.alipay.com/gateway.do";

    /**
     * 异步回调地址（notify_url）
     */
    private String notifyUrl;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }
}

