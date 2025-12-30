package com.example.takeout.web;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.service.OrderPaymentService;
import com.example.takeout.service.WechatPayFacade;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/wechatpay")
public class WechatPayNotifyController {

    private final Optional<WechatPayFacade> wechatPayFacadeOpt;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderPaymentService orderPaymentService;

    public WechatPayNotifyController(Optional<WechatPayFacade> wechatPayFacadeOpt,
                                     CustomerOrderRepository customerOrderRepository,
                                     OrderPaymentService orderPaymentService) {
        this.wechatPayFacadeOpt = wechatPayFacadeOpt;
        this.customerOrderRepository = customerOrderRepository;
        this.orderPaymentService = orderPaymentService;
    }

    @PostMapping("/notify")
    @Transactional
    public ResponseEntity<NotifyResponse> notify(@RequestHeader HttpHeaders headers, @RequestBody String body) {
        WechatPayFacade facade = wechatPayFacadeOpt.orElse(null);
        if (facade == null) {
            return ResponseEntity.ok(new NotifyResponse("FAIL", "wechatpay disabled"));
        }

        try {
            Transaction tx = facade.parseTransactionNotification(headers, body);
            if (tx == null) return ResponseEntity.ok(new NotifyResponse("FAIL", "empty transaction"));

            String outTradeNo = tx.getOutTradeNo();
            if (outTradeNo == null || outTradeNo.isBlank()) {
                return ResponseEntity.ok(new NotifyResponse("FAIL", "missing out_trade_no"));
            }

            Optional<CustomerOrder> orderOpt = customerOrderRepository.findFirstByPayOutTradeNo(outTradeNo.trim());
            if (orderOpt.isEmpty()) {
                return ResponseEntity.ok(new NotifyResponse("SUCCESS", "order not found"));
            }
            CustomerOrder order = orderOpt.get();

            Transaction.TradeStateEnum tradeState = tx.getTradeState();
            if (tradeState == null || tradeState != Transaction.TradeStateEnum.SUCCESS) {
                return ResponseEntity.ok(new NotifyResponse("SUCCESS", "ignored trade_state=" + tradeState));
            }

            Integer total = tx.getAmount() == null ? null : tx.getAmount().getTotal();
            int expectedFen = (order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount())
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .intValue();
            if (total == null || total != expectedFen) {
                return ResponseEntity.ok(new NotifyResponse("FAIL", "amount mismatch"));
            }

            orderPaymentService.markPaidIfNeeded(
                    order,
                    "WECHAT_NATIVE",
                    tx.getTransactionId(),
                    "SYSTEM",
                    null,
                    "微信支付回调"
            );

            return ResponseEntity.ok(new NotifyResponse("SUCCESS", "OK"));
        } catch (Exception e) {
            return ResponseEntity.ok(new NotifyResponse("FAIL", e.getMessage()));
        }
    }

    public static class NotifyResponse {
        private final String code;
        private final String message;

        public NotifyResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
