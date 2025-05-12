package com.example.sdpbackend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayHereConfig {
    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.return-url}")
    private String returnUrl;

    @Value("${payhere.cancel-url}")
    private String cancelUrl;

    @Value("${payhere.notify-url}")
    private String notifyUrl;

    @Value("${payhere.sandbox:true}")
    private boolean sandbox;

    @Value("${payhere.app-id}")
    private String appId;

    @Value("${payhere.app-secret}")
    private String appSecret;

    // Getters
    public String getMerchantId() {
        return merchantId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getBaseUrl() {
        return sandbox ? "https://sandbox.payhere.lk/pay/checkout" : "https://www.payhere.lk/pay/checkout";
    }
}
