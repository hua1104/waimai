package com.example.takeout.service;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.entity.PaymentLog;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.PaymentLogRepository;
import com.example.takeout.repository.PlatformConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderPaymentService {

    private final CustomerOrderRepository customerOrderRepository;
    private final PlatformConfigRepository platformConfigRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final String deliveryAssignmentMode;

    public OrderPaymentService(CustomerOrderRepository customerOrderRepository,
                               PlatformConfigRepository platformConfigRepository,
                               PaymentLogRepository paymentLogRepository,
                               DeliveryAssignmentService deliveryAssignmentService,
                               @Value("${delivery.assignment.mode:HALL}") String deliveryAssignmentMode) {
        this.customerOrderRepository = customerOrderRepository;
        this.platformConfigRepository = platformConfigRepository;
        this.paymentLogRepository = paymentLogRepository;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.deliveryAssignmentMode = deliveryAssignmentMode == null ? "HALL" : deliveryAssignmentMode.trim();
    }

    @Transactional
    public CustomerOrder markPaidIfNeeded(CustomerOrder order,
                                         String payMethod,
                                         String payTransactionId,
                                         String operatorRole,
                                         Long operatorId,
                                         String note) {
        if (order == null) throw new IllegalArgumentException("order is null");
        String payStatus = order.getPayStatus() == null ? "" : order.getPayStatus().trim().toUpperCase();
        if ("PAID".equals(payStatus)) return order;

        BigDecimal payAmount = order.getPayAmount();
        if (payAmount == null) {
            payAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            order.setPayAmount(payAmount);
        }

        order.setPayStatus("PAID");
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPayMethod(payMethod);
        if (payTransactionId != null && !payTransactionId.isBlank()) {
            order.setPayTransactionId(payTransactionId.trim());
        }

        BigDecimal rate = BigDecimal.ZERO;
        if (order.getRestaurant() != null && order.getRestaurant().getCommissionRate() != null) {
            rate = order.getRestaurant().getCommissionRate();
        } else {
            rate = platformConfigRepository.findById(1L)
                    .map(cfg -> cfg.getDefaultCommissionRate() == null ? BigDecimal.ZERO : cfg.getDefaultCommissionRate())
                    .orElse(BigDecimal.ZERO);
        }
        BigDecimal commission = payAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        order.setCommissionAmount(commission);

        CustomerOrder saved = customerOrderRepository.save(order);

        PaymentLog payLog = new PaymentLog();
        payLog.setOrder(saved);
        payLog.setType("PAY");
        payLog.setAmount(saved.getPayAmount() == null ? BigDecimal.ZERO : saved.getPayAmount());
        payLog.setMethod(payMethod == null ? "UNKNOWN" : payMethod);
        payLog.setOperatorRole(operatorRole);
        payLog.setOperatorId(operatorId);
        payLog.setStatus("SUCCESS");
        payLog.setNote(note);
        paymentLogRepository.save(payLog);

        if ("AUTO".equalsIgnoreCase(deliveryAssignmentMode)) {
            deliveryAssignmentService.autoAssignIfPossible(saved.getId());
        }

        return saved;
    }
}

