package com.example.takeout.service;

import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.entity.PaymentLog;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.PaymentLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAutoCloseService {

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;

    private final long unpaidTimeoutMinutes;
    private final long paidUnassignedAutoAssignMinutes;
    private final long paidUnassignedTimeoutMinutes;
    private final String deliveryAssignmentMode;

    public OrderAutoCloseService(CustomerOrderRepository customerOrderRepository,
                                 PaymentLogRepository paymentLogRepository,
                                 DeliveryAssignmentService deliveryAssignmentService,
                                 @Value("${order.timeout.unpaid-minutes:15}") long unpaidTimeoutMinutes,
                                 @Value("${order.timeout.paid-unassigned-auto-assign-minutes:0}") long paidUnassignedAutoAssignMinutes,
                                 @Value("${order.timeout.paid-unassigned-minutes:0}") long paidUnassignedTimeoutMinutes,
                                 @Value("${delivery.assignment.mode:HALL}") String deliveryAssignmentMode) {
        this.customerOrderRepository = customerOrderRepository;
        this.paymentLogRepository = paymentLogRepository;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.unpaidTimeoutMinutes = Math.max(1, unpaidTimeoutMinutes);
        this.paidUnassignedAutoAssignMinutes = Math.max(0, paidUnassignedAutoAssignMinutes);
        this.paidUnassignedTimeoutMinutes = Math.max(0, paidUnassignedTimeoutMinutes);
        this.deliveryAssignmentMode = deliveryAssignmentMode == null ? "HALL" : deliveryAssignmentMode.trim();
    }

    @Scheduled(fixedDelayString = "${order.timeout.job-interval-ms:60000}")
    @Transactional
    public void runOnce() {
        autoCancelUnpaid();
        autoAssignPaidUnassignedIfEnabled();
        autoCancelPaidUnassignedIfEnabled();
    }

    private void autoCancelUnpaid() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(unpaidTimeoutMinutes);
        List<CustomerOrder> stale = customerOrderRepository.findStaleUnpaidOrders(deadline);
        if (stale.isEmpty()) return;
        for (CustomerOrder o : stale) {
            o.setStatus("CANCELED");
            o.setCancelReason("支付超时，系统自动取消");
            o.setFinishedAt(now);
            customerOrderRepository.save(o);
        }
    }

    private void autoCancelPaidUnassignedIfEnabled() {
        if (paidUnassignedTimeoutMinutes <= 0) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(paidUnassignedTimeoutMinutes);
        List<CustomerOrder> stale = customerOrderRepository.findStalePaidUnassignedOrders(deadline);
        if (stale.isEmpty()) return;
        for (CustomerOrder o : stale) {
            o.setStatus("CANCELED");
            o.setCancelReason("长时间无人接单，系统自动取消并退款");
            o.setFinishedAt(now);
            if ("PAID".equalsIgnoreCase(o.getPayStatus())) {
                o.setPayStatus("REFUNDED");
                o.setRefundedAt(now);
                o.setCommissionAmount(BigDecimal.ZERO);

                PaymentLog refundLog = new PaymentLog();
                refundLog.setOrder(o);
                refundLog.setType("REFUND");
                refundLog.setAmount(o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount());
                refundLog.setMethod("MOCK");
                refundLog.setOperatorRole("SYSTEM");
                refundLog.setOperatorId(null);
                refundLog.setStatus("SUCCESS");
                refundLog.setNote("系统自动取消/退款");
                paymentLogRepository.save(refundLog);
            }
            customerOrderRepository.save(o);
        }
    }

    private void autoAssignPaidUnassignedIfEnabled() {
        if (paidUnassignedAutoAssignMinutes <= 0) return;
        if (!"HALL".equalsIgnoreCase(deliveryAssignmentMode)) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(paidUnassignedAutoAssignMinutes);
        List<CustomerOrder> stale = customerOrderRepository.findStalePaidUnassignedOrders(deadline);
        if (stale.isEmpty()) return;

        for (CustomerOrder o : stale) {
            DeliveryStaff staff = deliveryAssignmentService.autoAssignIfPossible(o.getId()).orElse(null);
            if (staff == null) continue;

            // 抢单超时后自动派单：进入配送中，方便骑手端默认列表查看
            customerOrderRepository.findById(o.getId()).ifPresent(updated -> {
                if (updated.getDeliveryStaff() != null && updated.getDeliveryStaff().getId() != null) {
                    updated.setStatus("DELIVERING");
                    customerOrderRepository.save(updated);
                }
            });
        }
    }
}
