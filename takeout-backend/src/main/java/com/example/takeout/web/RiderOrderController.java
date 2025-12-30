package com.example.takeout.web;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.entity.OrderItem;
import com.example.takeout.entity.Restaurant;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.DeliveryStaffRepository;
import com.example.takeout.repository.OrderItemRepository;
import com.example.takeout.repository.RestaurantRepository;
import com.example.takeout.service.BaiduMapService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rider")
public class RiderOrderController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final RestaurantRepository restaurantRepository;
    private final BaiduMapService baiduMapService;

    public RiderOrderController(CustomerOrderRepository customerOrderRepository,
                                OrderItemRepository orderItemRepository,
                                DeliveryStaffRepository deliveryStaffRepository,
                                RestaurantRepository restaurantRepository,
                                BaiduMapService baiduMapService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
        this.restaurantRepository = restaurantRepository;
        this.baiduMapService = baiduMapService;
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderListItem>> list(
            @RequestParam("deliveryStaffId") Long deliveryStaffId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "start", required = false) LocalDate start,
            @RequestParam(value = "end", required = false) LocalDate end
    ) {
        if (deliveryStaffId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startAt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endAt = end != null ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Page<CustomerOrderRepository.RiderOrderListProjection> projectionPage =
                customerOrderRepository.findRiderOrderList(deliveryStaffId, status, startAt, endAt, pageable);

        List<OrderListItem> items = projectionPage.getContent().stream()
                .map(p -> new OrderListItem(
                        p.getId(),
                        p.getRestaurantId(),
                        p.getRestaurantName(),
                        p.getCustomerId(),
                        p.getCustomerUsername(),
                        p.getDeliveryStaffId(),
                        p.getStatus(),
                        p.getPayStatus(),
                        p.getPayAmount(),
                        p.getCreatedAt(),
                        p.getPaidAt(),
                        p.getAddressDetail(),
                        p.getContactName(),
                        p.getContactPhone()
                ))
                .toList();

        return ResponseEntity.ok(new PageResponse<>(items, safePage, safeSize, projectionPage.getTotalElements()));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@RequestParam("deliveryStaffId") Long deliveryStaffId,
                                   @RequestParam(value = "date", required = false) LocalDate date) {
        if (deliveryStaffId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "deliveryStaffId不能为空"));
        }
        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(deliveryStaffId);
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "骑手不存在"));
        }
        DeliveryStaff staff = staffOpt.get();
        LocalDate target = date != null ? date : LocalDate.now();
        LocalDateTime startAt = target.atStartOfDay();
        LocalDateTime endAt = target.plusDays(1).atStartOfDay().minusNanos(1);

        long delivering = customerOrderRepository.countRiderDelivering(deliveryStaffId);
        long completedToday = customerOrderRepository.countRiderCompletedBetween(deliveryStaffId, startAt, endAt);
        double feePerOrder = 5.0;
        double incomeToday = completedToday * feePerOrder;

        return ResponseEntity.ok(Map.of(
                "deliveryStaffId", deliveryStaffId,
                "currentLoad", staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad(),
                "deliveringCount", delivering,
                "completedTodayCount", completedToday,
                "feePerOrder", feePerOrder,
                "incomeToday", incomeToday,
                "currentLat", staff.getCurrentLat(),
                "currentLng", staff.getCurrentLng(),
                "locationUpdatedAt", staff.getLocationUpdatedAt() != null ? staff.getLocationUpdatedAt().toString() : null
        ));
    }

    @GetMapping("/hall-orders")
    public ResponseEntity<PageResponse<HallOrderListItem>> hallOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "start", required = false) LocalDate start,
            @RequestParam(value = "end", required = false) LocalDate end
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startAt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endAt = end != null ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Page<CustomerOrderRepository.RiderHallOrderProjection> projectionPage =
                customerOrderRepository.findRiderHallOrders(startAt, endAt, pageable);

        List<HallOrderListItem> items = projectionPage.getContent().stream()
                .map(p -> new HallOrderListItem(
                        p.getId(),
                        p.getRestaurantId(),
                        p.getRestaurantName(),
                        p.getCustomerId(),
                        p.getCustomerUsername(),
                        p.getStatus(),
                        p.getPayStatus(),
                        p.getPayAmount(),
                        p.getCreatedAt(),
                        p.getPaidAt(),
                        p.getAddressDetail(),
                        p.getContactName(),
                        p.getContactPhone()
                ))
                .toList();

        return ResponseEntity.ok(new PageResponse<>(items, safePage, safeSize, projectionPage.getTotalElements()));
    }

    @GetMapping("/orders/{id}/route")
    public ResponseEntity<?> route(@PathVariable("id") Long id,
                                   @RequestParam("deliveryStaffId") Long deliveryStaffId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerOrder order = orderOpt.get();

        if (order.getDeliveryStaff() == null || order.getDeliveryStaff().getId() == null
                || !order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(deliveryStaffId);
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "骑手不存在"));
        }
        DeliveryStaff staff = staffOpt.get();

        Double riderLat = staff.getCurrentLat();
        Double riderLng = staff.getCurrentLng();
        if (riderLat == null || riderLng == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "请先上报骑手当前位置"));
        }

        Restaurant restaurant = order.getRestaurant();
        Double restLat = restaurant != null ? restaurant.getLat() : null;
        Double restLng = restaurant != null ? restaurant.getLng() : null;
        if ((restLat == null || restLng == null) && restaurant != null && restaurant.getAddress() != null) {
            baiduMapService.geocode(restaurant.getAddress()).ifPresent(coord -> {
                restaurant.setLat(coord.lat());
                restaurant.setLng(coord.lng());
                restaurantRepository.save(restaurant);
            });
            restLat = restaurant.getLat();
            restLng = restaurant.getLng();
        }

        Double customerLat = order.getDeliveryLat();
        Double customerLng = order.getDeliveryLng();
        if ((customerLat == null || customerLng == null) && order.getAddressDetail() != null) {
            baiduMapService.geocode(order.getAddressDetail()).ifPresent(coord -> {
                order.setDeliveryLat(coord.lat());
                order.setDeliveryLng(coord.lng());
                customerOrderRepository.save(order);
            });
            customerLat = order.getDeliveryLat();
            customerLng = order.getDeliveryLng();
        }

        if (restLat == null || restLng == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "无法获取饭店坐标（请补充饭店地址或开启百度AK）"));
        }
        if (customerLat == null || customerLng == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "无法获取收货坐标（请在下单时选择地点）"));
        }

        final double restLatV = restLat;
        final double restLngV = restLng;
        final double customerLatV = customerLat;
        final double customerLngV = customerLng;

        BaiduMapService.RouteInfo riderToRest =
                baiduMapService.drivingRoute(riderLat, riderLng, restLatV, restLngV)
                        .orElseGet(() -> BaiduMapService.haversineFallback(riderLat, riderLng, restLatV, restLngV));
        BaiduMapService.RouteInfo restToCustomer =
                baiduMapService.drivingRoute(restLatV, restLngV, customerLatV, customerLngV)
                        .orElseGet(() -> BaiduMapService.haversineFallback(restLatV, restLngV, customerLatV, customerLngV));
        BaiduMapService.RouteInfo riderToCustomer =
                baiduMapService.drivingRoute(riderLat, riderLng, customerLatV, customerLngV)
                        .orElseGet(() -> BaiduMapService.haversineFallback(riderLat, riderLng, customerLatV, customerLngV));

        return ResponseEntity.ok(Map.of(
                "points", Map.of(
                        "rider", Map.of("lat", riderLat, "lng", riderLng),
                        "restaurant", Map.of("lat", restLatV, "lng", restLngV),
                        "customer", Map.of("lat", customerLatV, "lng", customerLngV)
                ),
                "legs", Map.of(
                        "riderToRestaurant", Map.of("distanceKm", riderToRest.distanceKm(), "durationMin", riderToRest.durationMin(), "source", riderToRest.source()),
                        "restaurantToCustomer", Map.of("distanceKm", restToCustomer.distanceKm(), "durationMin", restToCustomer.durationMin(), "source", restToCustomer.source()),
                        "riderToCustomer", Map.of("distanceKm", riderToCustomer.distanceKm(), "durationMin", riderToCustomer.durationMin(), "source", riderToCustomer.source())
                ),
                "akEnabled", !baiduMapService.getAk().isEmpty()
        ));
    }

    @PostMapping("/orders/{id}/take")
    @Transactional
    public ResponseEntity<?> takeOrder(@PathVariable("id") Long id,
                                       @RequestParam("deliveryStaffId") Long deliveryStaffId) {
        if (deliveryStaffId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "deliveryStaffId不能为空"));
        }

        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(deliveryStaffId);
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "骑手不存在"));
        }
        DeliveryStaff staff = staffOpt.get();
        if (staff.getStatus() != null && !"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "骑手状态不可用"));
        }

        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerOrder order = orderOpt.get();

        if (order.getDeliveryStaff() != null && order.getDeliveryStaff().getId() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "订单已被其他骑手抢走"));
        }

        if (!"PAID".equalsIgnoreCase(order.getPayStatus()) || !"PAID".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "当前订单状态不可抢单"));
        }

        order.setDeliveryStaff(staff);
        order.setStatus("DELIVERING");
        customerOrderRepository.save(order);

        Integer load = staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad();
        staff.setCurrentLoad(load + 1);
        deliveryStaffRepository.save(staff);

        return ResponseEntity.ok(Map.of("status", "OK", "orderId", id, "statusAfter", "DELIVERING"));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDetail> detail(@PathVariable("id") Long id,
                                              @RequestParam("deliveryStaffId") Long deliveryStaffId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerOrder order = orderOpt.get();

        if (order.getDeliveryStaff() == null || order.getDeliveryStaff().getId() == null
                || !order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
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

        OrderDetail resp = new OrderDetail(
                order.getId(),
                order.getRestaurant() != null ? order.getRestaurant().getId() : null,
                order.getRestaurant() != null ? order.getRestaurant().getName() : null,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getUsername() : null,
                deliveryStaffId,
                order.getStatus(),
                order.getPayStatus(),
                order.getTotalAmount(),
                order.getPayAmount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getFinishedAt(),
                order.getAddressDetail(),
                order.getDeliveryLat(),
                order.getDeliveryLng(),
                order.getContactName(),
                order.getContactPhone(),
                itemRows
        );

        return ResponseEntity.ok(resp);
    }

    @PutMapping("/orders/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id,
                                          @RequestParam("deliveryStaffId") Long deliveryStaffId,
                                          @RequestBody StatusPayload payload) {
        if (payload == null || payload.getStatus() == null || payload.getStatus().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "status不能为空"));
        }

        String next = payload.getStatus().trim().toUpperCase();
        if (!"DELIVERING".equals(next) && !"COMPLETED".equals(next)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "骑手仅支持将订单置为 DELIVERING / COMPLETED"));
        }

        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        CustomerOrder order = orderOpt.get();

        if (order.getDeliveryStaff() == null || order.getDeliveryStaff().getId() == null
                || !order.getDeliveryStaff().getId().equals(deliveryStaffId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String current = order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase();

        if ("DELIVERING".equals(next)) {
            if (!"PAID".equals(current)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "仅当订单为 PAID 时才能开始配送"));
            }
            order.setStatus("DELIVERING");
            customerOrderRepository.save(order);
            return ResponseEntity.ok(Map.of("status", "OK"));
        }

        if (!"DELIVERING".equals(current)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "仅当订单为 DELIVERING 时才能完成配送"));
        }

        order.setStatus("COMPLETED");
        order.setFinishedAt(LocalDateTime.now());
        customerOrderRepository.save(order);

        decrementLoadIfPossible(order.getDeliveryStaff());

        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private void decrementLoadIfPossible(DeliveryStaff staff) {
        if (staff == null || staff.getId() == null) return;
        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(staff.getId());
        if (staffOpt.isEmpty()) return;
        DeliveryStaff s = staffOpt.get();
        Integer load = s.getCurrentLoad() == null ? 0 : s.getCurrentLoad();
        s.setCurrentLoad(Math.max(0, load - 1));
        deliveryStaffRepository.save(s);
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

    public static class HallOrderListItem {
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
        private final String addressDetail;
        private final String contactName;
        private final String contactPhone;

        public HallOrderListItem(Long id,
                                 Long restaurantId,
                                 String restaurantName,
                                 Long customerId,
                                 String customerUsername,
                                 String status,
                                 String payStatus,
                                 BigDecimal payAmount,
                                 LocalDateTime createdAt,
                                 LocalDateTime paidAt,
                                 String addressDetail,
                                 String contactName,
                                 String contactPhone) {
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
            this.addressDetail = addressDetail;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
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

        public String getAddressDetail() {
            return addressDetail;
        }

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }
    }

    public static class OrderListItem {
        private final Long id;
        private final Long restaurantId;
        private final String restaurantName;
        private final Long customerId;
        private final String customerUsername;
        private final Long deliveryStaffId;
        private final String status;
        private final String payStatus;
        private final BigDecimal payAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;
        private final String addressDetail;
        private final String contactName;
        private final String contactPhone;

        public OrderListItem(Long id,
                             Long restaurantId,
                             String restaurantName,
                             Long customerId,
                             String customerUsername,
                             Long deliveryStaffId,
                             String status,
                             String payStatus,
                             BigDecimal payAmount,
                             LocalDateTime createdAt,
                             LocalDateTime paidAt,
                             String addressDetail,
                             String contactName,
                             String contactPhone) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.customerId = customerId;
            this.customerUsername = customerUsername;
            this.deliveryStaffId = deliveryStaffId;
            this.status = status;
            this.payStatus = payStatus;
            this.payAmount = payAmount;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.addressDetail = addressDetail;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
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

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
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

        public String getAddressDetail() {
            return addressDetail;
        }

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }
    }

    public static class OrderDetail {
        private final Long id;
        private final Long restaurantId;
        private final String restaurantName;
        private final Long customerId;
        private final String customerUsername;
        private final Long deliveryStaffId;
        private final String status;
        private final String payStatus;
        private final BigDecimal totalAmount;
        private final BigDecimal payAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;
        private final LocalDateTime finishedAt;
        private final String addressDetail;
        private final Double deliveryLat;
        private final Double deliveryLng;
        private final String contactName;
        private final String contactPhone;
        private final List<OrderItemRow> items;

        public OrderDetail(Long id,
                           Long restaurantId,
                           String restaurantName,
                           Long customerId,
                           String customerUsername,
                           Long deliveryStaffId,
                           String status,
                           String payStatus,
                           BigDecimal totalAmount,
                           BigDecimal payAmount,
                           LocalDateTime createdAt,
                           LocalDateTime paidAt,
                           LocalDateTime finishedAt,
                           String addressDetail,
                           Double deliveryLat,
                           Double deliveryLng,
                           String contactName,
                           String contactPhone,
                           List<OrderItemRow> items) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.customerId = customerId;
            this.customerUsername = customerUsername;
            this.deliveryStaffId = deliveryStaffId;
            this.status = status;
            this.payStatus = payStatus;
            this.totalAmount = totalAmount;
            this.payAmount = payAmount;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.finishedAt = finishedAt;
            this.addressDetail = addressDetail;
            this.deliveryLat = deliveryLat;
            this.deliveryLng = deliveryLng;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
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

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
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
