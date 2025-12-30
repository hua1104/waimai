package com.example.takeout.web;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.entity.OrderItem;
import com.example.takeout.entity.PaymentLog;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.DeliveryStaffRepository;
import com.example.takeout.repository.OrderItemRepository;
import com.example.takeout.repository.PaymentLogRepository;
import com.example.takeout.service.DeliveryAssignmentService;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final PaymentLogRepository paymentLogRepository;

    public OrderController(CustomerOrderRepository customerOrderRepository,
                           OrderItemRepository orderItemRepository,
                           DeliveryStaffRepository deliveryStaffRepository,
                           DeliveryAssignmentService deliveryAssignmentService,
                           PaymentLogRepository paymentLogRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.paymentLogRepository = paymentLogRepository;
    }

    @GetMapping
    public PageResponse<OrderListItem> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "payStatus", required = false) String payStatus,
            @RequestParam(value = "restaurantId", required = false) Long restaurantId,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "start", required = false) LocalDate start,
            @RequestParam(value = "end", required = false) LocalDate end
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startAt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endAt = end != null ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Page<CustomerOrderRepository.OrderListProjection> projectionPage =
                customerOrderRepository.findOrderList(status, payStatus, restaurantId, customerId, startAt, endAt, pageable);

        List<OrderListItem> items = projectionPage.getContent().stream()
                .map(p -> new OrderListItem(
                        p.getId(),
                        p.getRestaurantId(),
                        p.getRestaurantName(),
                        p.getCustomerId(),
                        p.getCustomerUsername(),
                        p.getStatus(),
                        p.getPayStatus(),
                        p.getPayAmount(),
                        p.getCreatedAt(),
                        p.getPaidAt()
                ))
                .toList();

        return new PageResponse<>(items, safePage, safeSize, projectionPage.getTotalElements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetail> detail(@PathVariable("id") Long id) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CustomerOrder order = orderOpt.get();
        List<OrderItem> items = orderItemRepository.findByOrder_Id(id);
        List<OrderItemRow> itemRows = items.stream()
                .map(i -> new OrderItemRow(
                        i.getId(),
                        i.getDish() != null ? i.getDish().getId() : null,
                        i.getDishName(),
                        i.getUnitPrice(),
                        i.getQuantity()
                ))
                .toList();

        Long deliveryStaffId = null;
        String deliveryStaffName = null;
        String deliveryStaffPhone = null;
        if (order.getDeliveryStaff() != null) {
            deliveryStaffId = order.getDeliveryStaff().getId();
            deliveryStaffName = order.getDeliveryStaff().getName();
            deliveryStaffPhone = order.getDeliveryStaff().getPhone();
        }

        OrderDetail resp = new OrderDetail(
                order.getId(),
                order.getRestaurant() != null ? order.getRestaurant().getId() : null,
                order.getRestaurant() != null ? order.getRestaurant().getName() : null,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getUsername() : null,
                order.getStatus(),
                order.getPayStatus(),
                order.getTotalAmount(),
                order.getPayAmount(),
                order.getCommissionAmount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getFinishedAt(),
                order.getRefundedAt(),
                order.getCancelReason(),
                order.getAddressDetail(),
                order.getDeliveryLat(),
                order.getDeliveryLng(),
                order.getContactName(),
                order.getContactPhone(),
                deliveryStaffId,
                deliveryStaffName,
                deliveryStaffPhone,
                order.getDeliveryStaff() != null ? order.getDeliveryStaff().getCurrentLat() : null,
                order.getDeliveryStaff() != null ? order.getDeliveryStaff().getCurrentLng() : null,
                order.getDeliveryStaff() != null ? order.getDeliveryStaff().getCurrentLoad() : null,
                order.getDeliveryStaff() != null && order.getDeliveryStaff().getLocationUpdatedAt() != null ? order.getDeliveryStaff().getLocationUpdatedAt().toString() : null,
                itemRows
        );

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDetail> updateStatus(@PathVariable("id") Long id, @RequestBody StatusPayload payload) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerOrder order = orderOpt.get();
        if (payload.getStatus() == null || payload.getStatus().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String previous = order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase();
        String next = payload.getStatus().trim().toUpperCase();

        if ("COMPLETED".equals(previous) || "CANCELED".equals(previous)) {
            return ResponseEntity.status(409).build();
        }
        if (!List.of("CREATED", "PAID", "DELIVERING", "COMPLETED", "CANCELED").contains(next)) {
            return ResponseEntity.badRequest().build();
        }
        if ("REFUNDED".equalsIgnoreCase(order.getPayStatus()) && !"CANCELED".equals(next)) {
            return ResponseEntity.status(409).build();
        }
        if ("DELIVERING".equals(next) && (order.getDeliveryStaff() == null || order.getDeliveryStaff().getId() == null)) {
            return ResponseEntity.status(409).build();
        }

        order.setStatus(next);
        if ("COMPLETED".equals(next)) {
            order.setFinishedAt(LocalDateTime.now());
        }
        if ("CANCELED".equals(next)) {
            order.setFinishedAt(LocalDateTime.now());
            if (order.getCancelReason() == null || order.getCancelReason().isBlank()) {
                order.setCancelReason("后台取消");
            }
            if ("PAID".equalsIgnoreCase(order.getPayStatus())) {
                order.setPayStatus("REFUNDED");
                order.setRefundedAt(LocalDateTime.now());
                order.setCommissionAmount(BigDecimal.ZERO);

                PaymentLog refundLog = new PaymentLog();
                refundLog.setOrder(order);
                refundLog.setType("REFUND");
                refundLog.setAmount(order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
                refundLog.setMethod("MOCK");
                refundLog.setOperatorRole("ADMIN");
                refundLog.setOperatorId(null);
                refundLog.setStatus("SUCCESS");
                refundLog.setNote("后台取消/退款");
                paymentLogRepository.save(refundLog);
            }
            if (order.getDeliveryStaff() != null) {
                order.setDeliveryStaff(null);
            }
        }
        CustomerOrder saved = customerOrderRepository.save(order);

        if (("COMPLETED".equalsIgnoreCase(next) || "CANCELED".equalsIgnoreCase(next))
                && !("COMPLETED".equalsIgnoreCase(previous) || "CANCELED".equalsIgnoreCase(previous))
                && saved.getDeliveryStaff() != null
                && saved.getDeliveryStaff().getId() != null) {
            Long staffId = saved.getDeliveryStaff().getId();
            deliveryStaffRepository.findById(staffId).ifPresent(staff -> {
                Integer load = staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad();
                staff.setCurrentLoad(Math.max(0, load - 1));
                deliveryStaffRepository.save(staff);
            });
        }
        return detail(saved.getId());
    }

    @PostMapping("/{id}/assign-delivery")
    public ResponseEntity<?> assignDelivery(@PathVariable("id") Long id, @RequestBody AssignDeliveryPayload payload) {
        Long staffId = payload == null ? null : payload.getDeliveryStaffId();
        if (staffId == null) {
            return deliveryAssignmentService.autoAssignIfPossible(id)
                    .<ResponseEntity<?>>map(staff -> ResponseEntity.ok(Map.of("status", "OK", "deliveryStaffId", staff.getId())))
                    .orElseGet(() -> ResponseEntity.status(409).body(Map.of("message", "暂无可用骑手或订单状态不允许分配")));
        }
        return deliveryAssignmentService.assignOrReassign(id, staffId)
                .<ResponseEntity<?>>map(staff -> ResponseEntity.ok(Map.of("status", "OK", "deliveryStaffId", staff.getId())))
                .orElseGet(() -> ResponseEntity.status(409).body(Map.of("message", "分配失败：骑手不可用或订单状态不允许分配")));
    }

    public static class AssignDeliveryPayload {
        private Long deliveryStaffId;

        public AssignDeliveryPayload() {
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public void setDeliveryStaffId(Long deliveryStaffId) {
            this.deliveryStaffId = deliveryStaffId;
        }
    }

    public static class StatusPayload {
        private String status;

        public StatusPayload() {
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

    public static class OrderListItem {
        private final Long id;
        private final Long restaurantId;
        private final String restaurantName;
        private final Long customerId;
        private final String customerUsername;
        private final String status;
        private final String payStatus;
        private final BigDecimal payAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;

        public OrderListItem(Long id,
                             Long restaurantId,
                             String restaurantName,
                             Long customerId,
                             String customerUsername,
                             String status,
                             String payStatus,
                             BigDecimal payAmount,
                             LocalDateTime createdAt,
                             LocalDateTime paidAt) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.customerId = customerId;
            this.customerUsername = customerUsername;
            this.status = status;
            this.payStatus = payStatus;
            this.payAmount = payAmount;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
        }

        public Long getId() {
            return id;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public String getCustomerUsername() {
            return customerUsername;
        }

        public String getStatus() {
            return status;
        }

        public String getPayStatus() {
            return payStatus;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getPaidAt() {
            return paidAt;
        }
    }

    public static class OrderDetail {
        private final Long id;
        private final Long restaurantId;
        private final String restaurantName;
        private final Long customerId;
        private final String customerUsername;
        private final String status;
        private final String payStatus;
        private final BigDecimal totalAmount;
        private final BigDecimal payAmount;
        private final BigDecimal commissionAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;
        private final LocalDateTime finishedAt;
        private final LocalDateTime refundedAt;
        private final String cancelReason;
        private final String addressDetail;
        private final Double deliveryLat;
        private final Double deliveryLng;
        private final String contactName;
        private final String contactPhone;
        private final Long deliveryStaffId;
        private final String deliveryStaffName;
        private final String deliveryStaffPhone;
        private final Double deliveryStaffLat;
        private final Double deliveryStaffLng;
        private final Integer deliveryStaffLoad;
        private final String deliveryStaffLocationUpdatedAt;
        private final List<OrderItemRow> items;

        public OrderDetail(Long id,
                           Long restaurantId,
                           String restaurantName,
                           Long customerId,
                           String customerUsername,
                           String status,
                           String payStatus,
                           BigDecimal totalAmount,
                           BigDecimal payAmount,
                           BigDecimal commissionAmount,
                           LocalDateTime createdAt,
                           LocalDateTime paidAt,
                           LocalDateTime finishedAt,
                           LocalDateTime refundedAt,
                           String cancelReason,
                           String addressDetail,
                           Double deliveryLat,
                           Double deliveryLng,
                           String contactName,
                           String contactPhone,
                           Long deliveryStaffId,
                           String deliveryStaffName,
                           String deliveryStaffPhone,
                           Double deliveryStaffLat,
                           Double deliveryStaffLng,
                           Integer deliveryStaffLoad,
                           String deliveryStaffLocationUpdatedAt,
                           List<OrderItemRow> items) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.customerId = customerId;
            this.customerUsername = customerUsername;
            this.status = status;
            this.payStatus = payStatus;
            this.totalAmount = totalAmount;
            this.payAmount = payAmount;
            this.commissionAmount = commissionAmount;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.finishedAt = finishedAt;
            this.refundedAt = refundedAt;
            this.cancelReason = cancelReason;
            this.addressDetail = addressDetail;
            this.deliveryLat = deliveryLat;
            this.deliveryLng = deliveryLng;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.deliveryStaffId = deliveryStaffId;
            this.deliveryStaffName = deliveryStaffName;
            this.deliveryStaffPhone = deliveryStaffPhone;
            this.deliveryStaffLat = deliveryStaffLat;
            this.deliveryStaffLng = deliveryStaffLng;
            this.deliveryStaffLoad = deliveryStaffLoad;
            this.deliveryStaffLocationUpdatedAt = deliveryStaffLocationUpdatedAt;
            this.items = items;
        }

        public Long getId() {
            return id;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public String getCustomerUsername() {
            return customerUsername;
        }

        public String getStatus() {
            return status;
        }

        public String getPayStatus() {
            return payStatus;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }

        public BigDecimal getCommissionAmount() {
            return commissionAmount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getPaidAt() {
            return paidAt;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public LocalDateTime getRefundedAt() {
            return refundedAt;
        }

        public String getCancelReason() {
            return cancelReason;
        }

        public String getAddressDetail() {
            return addressDetail;
        }

        public Double getDeliveryLat() {
            return deliveryLat;
        }

        public Double getDeliveryLng() {
            return deliveryLng;
        }

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public String getDeliveryStaffName() {
            return deliveryStaffName;
        }

        public String getDeliveryStaffPhone() {
            return deliveryStaffPhone;
        }

        public Double getDeliveryStaffLat() {
            return deliveryStaffLat;
        }

        public Double getDeliveryStaffLng() {
            return deliveryStaffLng;
        }

        public Integer getDeliveryStaffLoad() {
            return deliveryStaffLoad;
        }

        public String getDeliveryStaffLocationUpdatedAt() {
            return deliveryStaffLocationUpdatedAt;
        }

        public List<OrderItemRow> getItems() {
            return items;
        }
    }

    public static class OrderItemRow {
        private final Long id;
        private final Long dishId;
        private final String dishName;
        private final BigDecimal unitPrice;
        private final Integer quantity;

        public OrderItemRow(Long id, Long dishId, String dishName, BigDecimal unitPrice, Integer quantity) {
            this.id = id;
            this.dishId = dishId;
            this.dishName = dishName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public Long getId() {
            return id;
        }

        public Long getDishId() {
            return dishId;
        }

        public String getDishName() {
            return dishName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }
}
