package com.example.takeout.web;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.service.OrderPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/mock-wechatpay")
public class MockWechatPayController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderPaymentService orderPaymentService;

    public MockWechatPayController(CustomerOrderRepository customerOrderRepository,
                                   OrderPaymentService orderPaymentService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderPaymentService = orderPaymentService;
    }

    @GetMapping(value = "/pay", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> payPage(@RequestParam("outTradeNo") String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing outTradeNo");
        }
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findFirstByPayOutTradeNo(outTradeNo.trim());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("order not found");
        }
        CustomerOrder order = orderOpt.get();
        String status = order.getPayStatus() == null ? "" : order.getPayStatus();

        String html = """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>模拟微信支付</title>
                  <style>
                    body{font-family:system-ui,-apple-system,"Segoe UI",Roboto,"PingFang SC","Microsoft YaHei",sans-serif;background:#f5f5f5;margin:0;padding:16px;}
                    .card{max-width:520px;margin:24px auto;background:#fff;border-radius:12px;padding:16px;box-shadow:0 8px 30px rgba(0,0,0,.08);}
                    .title{font-weight:700;font-size:18px;margin:0 0 8px;}
                    .sub{color:#666;font-size:13px;margin:0 0 12px;}
                    .row{display:flex;gap:10px;flex-wrap:wrap;color:#222;font-size:14px;margin:8px 0;}
                    .k{color:#666}
                    .btn{appearance:none;border:1px solid #d9d9d9;background:#fff;border-radius:8px;padding:10px 12px;font-size:14px;cursor:pointer}
                    .btn.primary{border-color:#07c160;background:#07c160;color:#fff}
                    .msg{margin-top:10px;font-size:13px;color:#333}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <p class="title">模拟微信支付（扫码确认）</p>
                    <p class="sub">用于无商户号场景的演示闭环：扫码后点击“确认支付”，后端将订单置为已支付。</p>
                    <div class="row"><span class="k">订单号：</span><span id="oid"></span></div>
                    <div class="row"><span class="k">支付状态：</span><span id="ps"></span></div>
                    <div class="row"><span class="k">当前时间：</span><span>%s</span></div>
                    <div style="display:flex;gap:10px;margin-top:12px;">
                      <button class="btn primary" id="confirm">确认支付</button>
                      <button class="btn" id="refresh">刷新状态</button>
                    </div>
                    <div class="msg" id="msg"></div>
                  </div>
                  <script>
                    const outTradeNo = %s;
                    const oid = %s;
                    const ps = %s;
                    document.getElementById('oid').textContent = oid;
                    document.getElementById('ps').textContent = ps;
                    const msgEl = document.getElementById('msg');

                    async function confirmPay(){
                      msgEl.textContent = '提交中...';
                      const res = await fetch(`/api/mock-wechatpay/confirm?outTradeNo=${encodeURIComponent(outTradeNo)}`, {method:'POST'});
                      const data = await res.json().catch(()=>({}));
                      if(!res.ok){ msgEl.textContent = (data && data.message) ? data.message : '失败'; return; }
                      msgEl.textContent = '已确认支付，返回订单页查看状态即可。';
                      await refresh();
                    }
                    async function refresh(){
                      const res = await fetch(`/api/mock-wechatpay/status?outTradeNo=${encodeURIComponent(outTradeNo)}`);
                      const data = await res.json().catch(()=>({}));
                      if(res.ok && data && data.payStatus){
                        document.getElementById('ps').textContent = data.payStatus;
                      }
                    }
                    document.getElementById('confirm').addEventListener('click', confirmPay);
                    document.getElementById('refresh').addEventListener('click', refresh);
                  </script>
                </body>
                </html>
                """.formatted(LocalDateTime.now(), json(outTradeNo.trim()), json(order.getId()), json(status));

        return ResponseEntity.ok(html);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam("outTradeNo") String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("missing outTradeNo"));
        }
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findFirstByPayOutTradeNo(outTradeNo.trim());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Message("order not found"));
        }
        CustomerOrder order = orderOpt.get();
        return ResponseEntity.ok(new StatusResponse(order.getId(), order.getPayStatus(), order.getPayAmount()));
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirm(@RequestParam("outTradeNo") String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("missing outTradeNo"));
        }
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findFirstByPayOutTradeNo(outTradeNo.trim());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Message("order not found"));
        }
        CustomerOrder order = orderOpt.get();
        orderPaymentService.markPaidIfNeeded(
                order,
                "WECHAT_MOCK_QR",
                "MOCKTX-" + System.currentTimeMillis(),
                "SYSTEM",
                null,
                "模拟微信扫码确认"
        );
        return ResponseEntity.ok(new Message("OK"));
    }

    private static String json(Object v) {
        if (v == null) return "null";
        String s = String.valueOf(v);
        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    public static class Message {
        private final String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StatusResponse {
        private final Long orderId;
        private final String payStatus;
        private final Object payAmount;

        public StatusResponse(Long orderId, String payStatus, Object payAmount) {
            this.orderId = orderId;
            this.payStatus = payStatus;
            this.payAmount = payAmount;
        }

        public Long getOrderId() {
            return orderId;
        }

        public String getPayStatus() {
            return payStatus;
        }

        public Object getPayAmount() {
            return payAmount;
        }
    }
}

