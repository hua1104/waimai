package com.example.takeout.web;

import com.example.takeout.entity.*;
import com.example.takeout.repository.*;
import com.example.takeout.service.OrderPaymentService;
import com.example.takeout.service.QrCodeService;
import com.example.takeout.service.WechatPayFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
public class CustomerOrderController {

    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final RestaurantPromotionRepository restaurantPromotionRepository;
    private final OrderPaymentService orderPaymentService;
    private final PaymentLogRepository paymentLogRepository;
    private final QrCodeService qrCodeService;
    private final Optional<WechatPayFacade> wechatPayFacadeOpt;
    private final String appPublicBaseUrl;

    public CustomerOrderController(CustomerRepository customerRepository,
                                   RestaurantRepository restaurantRepository,
                                   DishRepository dishRepository,
                                   CustomerOrderRepository customerOrderRepository,
                                   OrderItemRepository orderItemRepository,
                                   DeliveryStaffRepository deliveryStaffRepository,
                                   RestaurantPromotionRepository restaurantPromotionRepository,
                                   OrderPaymentService orderPaymentService,
                                   PaymentLogRepository paymentLogRepository,
                                   QrCodeService qrCodeService,
                                   Optional<WechatPayFacade> wechatPayFacadeOpt,
                                   @Value("${app.publicBaseUrl:http://localhost:8081}") String appPublicBaseUrl) {
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
        this.restaurantPromotionRepository = restaurantPromotionRepository;
        this.orderPaymentService = orderPaymentService;
        this.paymentLogRepository = paymentLogRepository;
        this.qrCodeService = qrCodeService;
        this.wechatPayFacadeOpt = wechatPayFacadeOpt;
        this.appPublicBaseUrl = appPublicBaseUrl == null ? "http://localhost:8081" : appPublicBaseUrl.trim();
    }

    @PostMapping("/orders")
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        if (request.getCustomerId() == null || request.getRestaurantId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("参数不完整"));
        }

        Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("食客不存在"));
        }
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(request.getRestaurantId());
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("饭店不存在"));
        }

        Customer customer = customerOpt.get();
        Restaurant restaurant = restaurantOpt.get();

        BigDecimal totalOriginal = BigDecimal.ZERO;
        BigDecimal totalPayBeforePromotion = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItem item : request.getItems()) {
            if (item.getDishId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("菜品或数量不合法"));
            }
            Optional<Dish> dishOpt = dishRepository.findById(item.getDishId());
            if (dishOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("菜品不存在: " + item.getDishId()));
            }
            Dish dish = dishOpt.get();
            if (dish.getRestaurant() == null || dish.getRestaurant().getId() == null || !dish.getRestaurant().getId().equals(restaurant.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("菜品不属于该饭店"));
            }
            if (!"AVAILABLE".equalsIgnoreCase(dish.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("菜品已下架: " + dish.getName()));
            }

            BigDecimal originalUnit = dish.getPrice() != null ? dish.getPrice() : BigDecimal.ZERO;
            BigDecimal payUnit = calcPayUnitPrice(dish, originalUnit);

            totalOriginal = totalOriginal.add(originalUnit.multiply(BigDecimal.valueOf(item.getQuantity())));
            totalPayBeforePromotion = totalPayBeforePromotion.add(payUnit.multiply(BigDecimal.valueOf(item.getQuantity())));

            OrderItem oi = new OrderItem();
            oi.setDish(dish);
            oi.setDishName(dish.getName());
            oi.setUnitPrice(payUnit);
            oi.setQuantity(item.getQuantity());
            orderItems.add(oi);
        }

        BigDecimal promoDiscount = calculatePromotionDiscount(restaurant.getId(), totalPayBeforePromotion);
        BigDecimal payAmount = totalPayBeforePromotion.subtract(promoDiscount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) payAmount = BigDecimal.ZERO;

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(totalOriginal);
        order.setPayAmount(payAmount);
        order.setCommissionAmount(null);
        order.setDiscountAmount(totalOriginal.subtract(payAmount));
        order.setStatus("CREATED");
        order.setPayStatus("UNPAID");
        order.setCreatedAt(LocalDateTime.now());
        order.setAddressDetail(request.getAddressDetail());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setRemark(safe(request.getRemark(), 255));
        order.setDeliveryLat(request.getDeliveryLat());
        order.setDeliveryLng(request.getDeliveryLng());

        CustomerOrder savedOrder = customerOrderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateOrderResponse(
                savedOrder.getId(),
                totalOriginal,
                order.getDiscountAmount(),
                payAmount
        ));
    }

    @PostMapping("/orders/{id}/pay")
    @Transactional
    public ResponseEntity<?> pay(@PathVariable("id") Long orderId,
                                 @RequestParam(value = "method", required = false) String method) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerOrder order = orderOpt.get();
        if (!"UNPAID".equalsIgnoreCase(order.getPayStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("订单不是待支付状态"));
        }

        String m = method == null ? "" : method.trim();
        if ("WECHAT_MOCK_QR".equalsIgnoreCase(m)) {
            BigDecimal payAmount = order.getPayAmount();
            if (payAmount == null) {
                payAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                order.setPayAmount(payAmount);
            }
            if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("Invalid pay amount"));
            }

            String outTradeNo = order.getPayOutTradeNo();
            if (outTradeNo == null || outTradeNo.isBlank()) {
                outTradeNo = "MOCKO" + order.getId() + "T" + System.currentTimeMillis();
                order.setPayOutTradeNo(outTradeNo);
            }

            String base = appPublicBaseUrl.endsWith("/") ? appPublicBaseUrl.substring(0, appPublicBaseUrl.length() - 1) : appPublicBaseUrl;
            String codeUrl = base + "/api/mock-wechatpay/pay?outTradeNo=" + URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8);
            order.setPayCodeUrl(codeUrl);
            order.setPayMethod("WECHAT_MOCK_QR");
            customerOrderRepository.save(order);

            PaymentLog payLog = new PaymentLog();
            payLog.setOrder(order);
            payLog.setType("PAY");
            payLog.setAmount(order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
            payLog.setMethod("WECHAT_MOCK_QR");
            payLog.setOperatorRole("CUSTOMER");
            payLog.setOperatorId(order.getCustomer() != null ? order.getCustomer().getId() : null);
            payLog.setStatus("PENDING");
            payLog.setNote("mock wechat out_trade_no=" + outTradeNo);
            paymentLogRepository.save(payLog);

            byte[] png = qrCodeService.toPng(codeUrl, 260);
            String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
            return ResponseEntity.ok(new WechatNativePayResponse(order.getId(), order.getPayAmount(), outTradeNo, codeUrl, dataUrl));
        }

        if ("WECHAT_NATIVE".equalsIgnoreCase(m) || "WECHAT".equalsIgnoreCase(m) || "WXPAY".equalsIgnoreCase(m)) {
            WechatPayFacade wechatPayFacade = wechatPayFacadeOpt.orElse(null);
            if (wechatPayFacade == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("WeChat Pay is not enabled (try method=WECHAT_MOCK_QR)"));
            }

            BigDecimal payAmount = order.getPayAmount();
            if (payAmount == null) {
                payAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                order.setPayAmount(payAmount);
            }
            int totalFen = payAmount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .intValue();
            if (totalFen <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("Invalid pay amount"));
            }

            String outTradeNo = order.getPayOutTradeNo();
            if (outTradeNo == null || outTradeNo.isBlank()) {
                outTradeNo = "O" + order.getId() + "T" + System.currentTimeMillis();
                order.setPayOutTradeNo(outTradeNo);
            }

            String codeUrl = order.getPayCodeUrl();
            if (codeUrl == null || codeUrl.isBlank()) {
                codeUrl = wechatPayFacade.prepayNative(outTradeNo, "Order " + order.getId(), totalFen);
                if (codeUrl == null || codeUrl.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new Message("Failed to create WeChat Pay order"));
                }
                order.setPayCodeUrl(codeUrl);
            }
            order.setPayMethod("WECHAT_NATIVE");
            customerOrderRepository.save(order);

            PaymentLog payLog = new PaymentLog();
            payLog.setOrder(order);
            payLog.setType("PAY");
            payLog.setAmount(order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
            payLog.setMethod("WECHAT_NATIVE");
            payLog.setOperatorRole("CUSTOMER");
            payLog.setOperatorId(order.getCustomer() != null ? order.getCustomer().getId() : null);
            payLog.setStatus("PENDING");
            payLog.setNote("wechat out_trade_no=" + outTradeNo);
            paymentLogRepository.save(payLog);

            byte[] png = qrCodeService.toPng(codeUrl, 260);
            String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
            return ResponseEntity.ok(new WechatNativePayResponse(order.getId(), order.getPayAmount(), outTradeNo, codeUrl, dataUrl));
        }

        CustomerOrder paid = orderPaymentService.markPaidIfNeeded(
                order,
                "MOCK",
                null,
                "CUSTOMER",
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                "模拟支付"
        );
        return ResponseEntity.ok(new PayResponse(
                paid.getId(),
                paid.getTotalAmount(),
                paid.getDiscountAmount(),
                paid.getPayAmount(),
                paid.getCommissionAmount(),
                paid.getStatus()
        ));
    }

    private BigDecimal calculatePromotionDiscount(Long restaurantId, BigDecimal payBeforePromotion) {
        if (restaurantId == null) return BigDecimal.ZERO;
        if (payBeforePromotion == null) return BigDecimal.ZERO;
        if (payBeforePromotion.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        LocalDateTime now = LocalDateTime.now();
        List<RestaurantPromotion> list = restaurantPromotionRepository.findActiveAt(restaurantId, now);
        if (list.isEmpty()) return BigDecimal.ZERO;

        BigDecimal best = BigDecimal.ZERO;
        for (RestaurantPromotion p : list) {
            if (!"FULL_REDUCTION".equalsIgnoreCase(p.getType())) continue;
            BigDecimal threshold = p.getThresholdAmount() == null ? BigDecimal.ZERO : p.getThresholdAmount();
            BigDecimal discount = p.getDiscountAmount() == null ? BigDecimal.ZERO : p.getDiscountAmount();
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (payBeforePromotion.compareTo(threshold) >= 0) {
                if (discount.compareTo(best) > 0) best = discount;
            }
        }
        if (best.compareTo(payBeforePromotion) > 0) return payBeforePromotion;
        return best;
    }

    private static BigDecimal calcPayUnitPrice(Dish dish, BigDecimal originalUnit) {
        if (originalUnit == null) return BigDecimal.ZERO;
        if (dish == null) return originalUnit;
        String t = dish.getDiscountType();
        BigDecimal v = dish.getDiscountValue();
        if (t == null || t.isBlank() || v == null) return originalUnit;

        if ("PERCENT".equalsIgnoreCase(t)) {
            BigDecimal ratio;
            if (v.compareTo(BigDecimal.ONE) <= 0) {
                ratio = v;
            } else {
                ratio = v.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
            }
            BigDecimal pay = originalUnit.multiply(ratio);
            if (pay.compareTo(BigDecimal.ZERO) < 0) pay = BigDecimal.ZERO;
            return pay.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if ("AMOUNT".equalsIgnoreCase(t)) {
            BigDecimal pay = originalUnit.subtract(v);
            if (pay.compareTo(BigDecimal.ZERO) < 0) pay = BigDecimal.ZERO;
            return pay.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return originalUnit;
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderListItem>> myOrders(
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "start", required = false) LocalDate start,
            @RequestParam(value = "end", required = false) LocalDate end
    ) {
        if (!customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startAt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endAt = end != null ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Page<CustomerOrderRepository.OrderListProjection> projectionPage =
                customerOrderRepository.findOrderList(status, null, null, customerId, startAt, endAt, pageable);

        List<OrderListItem> items = projectionPage.getContent().stream()
                .map(p -> new OrderListItem(
                        p.getId(),
                        p.getRestaurantId(),
                        p.getRestaurantName(),
                        p.getStatus(),
                        p.getPayStatus(),
                        p.getPayAmount(),
                        p.getCreatedAt(),
                        p.getPaidAt()
                ))
                .toList();

        return ResponseEntity.ok(new PageResponse<>(items, safePage, safeSize, projectionPage.getTotalElements()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDetail> myOrderDetail(@PathVariable("id") Long id,
                                                     @RequestParam("customerId") Long customerId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerOrder order = orderOpt.get();
        if (order.getCustomer() == null || order.getCustomer().getId() == null || !order.getCustomer().getId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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
                order.getStatus(),
                order.getPayStatus(),
                order.getTotalAmount(),
                order.getPayAmount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getFinishedAt(),
                order.getRefundedAt(),
                order.getCancelReason(),
                order.getAddressDetail(),
                order.getContactName(),
                order.getContactPhone(),
                order.getRemark(),
                deliveryStaffId,
                deliveryStaffName,
                deliveryStaffPhone,
                itemRows
        );

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/orders/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable("id") Long orderId, @RequestBody CancelRequest request) {
        if (request == null || request.getCustomerId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("参数不完整"));
        }

        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CustomerOrder order = orderOpt.get();
        if (order.getCustomer() == null || order.getCustomer().getId() == null
                || !order.getCustomer().getId().equals(request.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String status = order.getStatus() == null ? "" : order.getStatus();
        String payStatus = order.getPayStatus() == null ? "" : order.getPayStatus();

        if ("CANCELED".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(new Message("OK"));
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Message("订单已完成，无法取消/退款"));
        }
        if ("DELIVERING".equalsIgnoreCase(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Message("订单配送中，无法取消/退款"));
        }
        if (!"CREATED".equalsIgnoreCase(status) && !"PAID".equalsIgnoreCase(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Message("当前状态不支持取消"));
        }
        if ("PAID".equalsIgnoreCase(status) && !"PAID".equalsIgnoreCase(payStatus)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Message("支付状态异常，无法退款"));
        }

        if (order.getDeliveryStaff() != null) {
            DeliveryStaff staff = order.getDeliveryStaff();
            Integer load = staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad();
            staff.setCurrentLoad(Math.max(0, load - 1));
            deliveryStaffRepository.save(staff);
            order.setDeliveryStaff(null);
        }

        if ("PAID".equalsIgnoreCase(payStatus)) {
            order.setPayStatus("REFUNDED");
            order.setRefundedAt(LocalDateTime.now());
            order.setCommissionAmount(BigDecimal.ZERO);

            PaymentLog refundLog = new PaymentLog();
            refundLog.setOrder(order);
            refundLog.setType("REFUND");
            refundLog.setAmount(order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
            refundLog.setMethod("MOCK");
            refundLog.setOperatorRole("CUSTOMER");
            refundLog.setOperatorId(order.getCustomer() != null ? order.getCustomer().getId() : null);
            refundLog.setStatus("SUCCESS");
            refundLog.setNote("模拟退款");
            paymentLogRepository.save(refundLog);
        }
        order.setStatus("CANCELED");
        order.setCancelReason(safe(request.getReason(), 255));
        order.setFinishedAt(LocalDateTime.now());
        customerOrderRepository.save(order);

        return ResponseEntity.ok(new Message("OK"));
    }

    private static String safe(String v, int maxLen) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    public static class CreateOrderRequest {
        private Long customerId;
        private Long restaurantId;
        private String addressDetail;
        private String contactName;
        private String contactPhone;
        private String remark;
        private Double deliveryLat;
        private Double deliveryLng;
        private List<CreateOrderItem> items;

        public CreateOrderRequest() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }

        public String getAddressDetail() {
            return addressDetail;
        }

        public void setAddressDetail(String addressDetail) {
            this.addressDetail = addressDetail;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getDeliveryLat() {
            return deliveryLat;
        }

        public void setDeliveryLat(Double deliveryLat) {
            this.deliveryLat = deliveryLat;
        }

        public Double getDeliveryLng() {
            return deliveryLng;
        }

        public void setDeliveryLng(Double deliveryLng) {
            this.deliveryLng = deliveryLng;
        }

        public List<CreateOrderItem> getItems() {
            return items;
        }

        public void setItems(List<CreateOrderItem> items) {
            this.items = items;
        }
    }

    public static class CreateOrderItem {
        private Long dishId;
        private Integer quantity;

        public CreateOrderItem() {
        }

        public Long getDishId() {
            return dishId;
        }

        public void setDishId(Long dishId) {
            this.dishId = dishId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class CreateOrderResponse {
        private Long orderId;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal payAmount;

        public CreateOrderResponse(Long orderId, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal payAmount) {
            this.orderId = orderId;
            this.totalAmount = totalAmount;
            this.discountAmount = discountAmount;
            this.payAmount = payAmount;
        }

        public Long getOrderId() {
            return orderId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }
    }

    public static class PayResponse {
        private Long orderId;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal payAmount;
        private BigDecimal commissionAmount;
        private String status;

        public PayResponse(Long orderId,
                           BigDecimal totalAmount,
                           BigDecimal discountAmount,
                           BigDecimal payAmount,
                           BigDecimal commissionAmount,
                           String status) {
            this.orderId = orderId;
            this.totalAmount = totalAmount;
            this.discountAmount = discountAmount;
            this.payAmount = payAmount;
            this.commissionAmount = commissionAmount;
            this.status = status;
        }

        public Long getOrderId() {
            return orderId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }

        public BigDecimal getCommissionAmount() {
            return commissionAmount;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class WechatNativePayResponse {
        private final Long orderId;
        private final BigDecimal payAmount;
        private final String outTradeNo;
        private final String codeUrl;
        private final String qrCodeDataUrl;

        public WechatNativePayResponse(Long orderId,
                                      BigDecimal payAmount,
                                      String outTradeNo,
                                      String codeUrl,
                                      String qrCodeDataUrl) {
            this.orderId = orderId;
            this.payAmount = payAmount;
            this.outTradeNo = outTradeNo;
            this.codeUrl = codeUrl;
            this.qrCodeDataUrl = qrCodeDataUrl;
        }

        public Long getOrderId() {
            return orderId;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getCodeUrl() {
            return codeUrl;
        }

        public String getQrCodeDataUrl() {
            return qrCodeDataUrl;
        }
    }

    public static class CancelRequest {
        private Long customerId;
        private String reason;

        public CancelRequest() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
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
        private final String status;
        private final String payStatus;
        private final BigDecimal payAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;

        public OrderListItem(Long id,
                             Long restaurantId,
                             String restaurantName,
                             String status,
                             String payStatus,
                             BigDecimal payAmount,
                             LocalDateTime createdAt,
                             LocalDateTime paidAt) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
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
        private final String status;
        private final String payStatus;
        private final BigDecimal totalAmount;
        private final BigDecimal payAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;
        private final LocalDateTime finishedAt;
        private final LocalDateTime refundedAt;
        private final String cancelReason;
        private final String addressDetail;
        private final String contactName;
        private final String contactPhone;
        private final String remark;
        private final Long deliveryStaffId;
        private final String deliveryStaffName;
        private final String deliveryStaffPhone;
        private final List<OrderItemRow> items;

        public OrderDetail(Long id,
                           Long restaurantId,
                           String restaurantName,
                           String status,
                           String payStatus,
                           BigDecimal totalAmount,
                           BigDecimal payAmount,
                           LocalDateTime createdAt,
                           LocalDateTime paidAt,
                           LocalDateTime finishedAt,
                           LocalDateTime refundedAt,
                           String cancelReason,
                           String addressDetail,
                           String contactName,
                           String contactPhone,
                           String remark,
                           Long deliveryStaffId,
                           String deliveryStaffName,
                           String deliveryStaffPhone,
                           List<OrderItemRow> items) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.status = status;
            this.payStatus = payStatus;
            this.totalAmount = totalAmount;
            this.payAmount = payAmount;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.finishedAt = finishedAt;
            this.refundedAt = refundedAt;
            this.cancelReason = cancelReason;
            this.addressDetail = addressDetail;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.remark = remark;
            this.deliveryStaffId = deliveryStaffId;
            this.deliveryStaffName = deliveryStaffName;
            this.deliveryStaffPhone = deliveryStaffPhone;
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

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public String getRemark() {
            return remark;
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

    public static class Message {
        private String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
