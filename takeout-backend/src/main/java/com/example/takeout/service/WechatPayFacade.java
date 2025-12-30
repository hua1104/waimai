package com.example.takeout.service;

import com.example.takeout.config.WechatPayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@ConditionalOnProperty(prefix = "wechatpay", name = "enabled", havingValue = "true")
public class WechatPayFacade {

    private final WechatPayProperties props;
    private final NativePayService nativePayService;
    private final NotificationParser notificationParser;

    public WechatPayFacade(WechatPayProperties props) {
        this.props = props;
        String mchId = required("wechatpay.mchId", props.getMchId());
        String privateKeyPath = required("wechatpay.privateKeyPath", props.getPrivateKeyPath());
        String mchSerialNo = required("wechatpay.mchSerialNo", props.getMchSerialNo());
        String apiV3Key = required("wechatpay.apiV3Key", props.getApiV3Key());

        Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(mchSerialNo)
                .apiV3Key(apiV3Key)
                .build();
        this.nativePayService = new NativePayService.Builder().config(config).build();

        NotificationConfig notificationConfig = new AutoCertificateNotificationConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(mchSerialNo)
                .apiV3Key(apiV3Key)
                .build();
        this.notificationParser = new NotificationParser(notificationConfig);
    }

    public String prepayNative(String outTradeNo, String description, int totalFen) {
        String notifyUrl = required("wechatpay.notifyUrl", props.getNotifyUrl());
        PrepayRequest request = new PrepayRequest();
        request.setAppid(required("wechatpay.appId", props.getAppId()));
        request.setMchid(required("wechatpay.mchId", props.getMchId()));
        request.setDescription(description == null ? "Takeout Order" : description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(notifyUrl);
        Amount amount = new Amount();
        amount.setTotal(totalFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        PrepayResponse resp = nativePayService.prepay(request);
        return resp == null ? null : resp.getCodeUrl();
    }

    public Transaction parseTransactionNotification(HttpHeaders headers, String body) {
        RequestParam param = new RequestParam.Builder()
                .serialNumber(Objects.requireNonNull(headers.getFirst("Wechatpay-Serial"), "missing Wechatpay-Serial"))
                .nonce(Objects.requireNonNull(headers.getFirst("Wechatpay-Nonce"), "missing Wechatpay-Nonce"))
                .signature(Objects.requireNonNull(headers.getFirst("Wechatpay-Signature"), "missing Wechatpay-Signature"))
                .timestamp(Objects.requireNonNull(headers.getFirst("Wechatpay-Timestamp"), "missing Wechatpay-Timestamp"))
                .body(body)
                .build();
        return notificationParser.parse(param, Transaction.class);
    }

    private static String required(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value.trim();
    }
}
