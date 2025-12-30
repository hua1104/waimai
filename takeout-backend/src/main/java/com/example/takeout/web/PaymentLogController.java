package com.example.takeout.web;

import com.example.takeout.repository.PaymentLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/payment-logs")
public class PaymentLogController {

    private final PaymentLogRepository paymentLogRepository;

    public PaymentLogController(PaymentLogRepository paymentLogRepository) {
        this.paymentLogRepository = paymentLogRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PaymentLogRow>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "start", required = false) LocalDate start,
            @RequestParam(value = "end", required = false) LocalDate end
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 200);

        LocalDateTime startAt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endAt = end != null ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentLogRepository.PaymentLogListProjection> p =
                paymentLogRepository.findLogs(orderId, blankToNull(type), startAt, endAt, pageable);

        List<PaymentLogRow> items = p.getContent().stream()
                .map(x -> new PaymentLogRow(
                        x.getId(),
                        x.getOrderId(),
                        x.getType(),
                        x.getAmount(),
                        x.getMethod(),
                        x.getOperatorRole(),
                        x.getOperatorId(),
                        x.getStatus(),
                        x.getNote(),
                        x.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(new PageResponse<>(items, safePage, safeSize, p.getTotalElements()));
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public static class PaymentLogRow {
        private final Long id;
        private final Long orderId;
        private final String type;
        private final BigDecimal amount;
        private final String method;
        private final String operatorRole;
        private final Long operatorId;
        private final String status;
        private final String note;
        private final LocalDateTime createdAt;

        public PaymentLogRow(Long id,
                             Long orderId,
                             String type,
                             BigDecimal amount,
                             String method,
                             String operatorRole,
                             Long operatorId,
                             String status,
                             String note,
                             LocalDateTime createdAt) {
            this.id = id;
            this.orderId = orderId;
            this.type = type;
            this.amount = amount;
            this.method = method;
            this.operatorRole = operatorRole;
            this.operatorId = operatorId;
            this.status = status;
            this.note = note;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public Long getOrderId() {
            return orderId;
        }

        public String getType() {
            return type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getMethod() {
            return method;
        }

        public String getOperatorRole() {
            return operatorRole;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public String getStatus() {
            return status;
        }

        public String getNote() {
            return note;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public static class PageResponse<T> {
        private final List<T> items;
        private final int page;
        private final int size;
        private final long total;

        public PageResponse(List<T> items, int page, int size, long total) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.total = total;
        }

        public List<T> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public long getTotal() {
            return total;
        }
    }
}

