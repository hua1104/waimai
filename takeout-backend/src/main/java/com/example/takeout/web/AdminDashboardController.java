package com.example.takeout.web;

import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.CustomerRepository;
import com.example.takeout.repository.DeliveryStaffRepository;
import com.example.takeout.repository.RestaurantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final RestaurantRepository restaurantRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public AdminDashboardController(RestaurantRepository restaurantRepository,
                                   CustomerRepository customerRepository,
                                   DeliveryStaffRepository deliveryStaffRepository,
                                   CustomerOrderRepository customerOrderRepository) {
        this.restaurantRepository = restaurantRepository;
        this.customerRepository = customerRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate safeEnd = end != null ? end : LocalDate.now();
        LocalDate safeStart = start != null ? start : safeEnd.minusDays(6);

        LocalDateTime startAt = safeStart.atStartOfDay();
        LocalDateTime endAt = safeEnd.plusDays(1).atStartOfDay().minusNanos(1);

        long restaurantCount = restaurantRepository.count();
        long customerCount = customerRepository.count();
        long deliveryStaffCount = deliveryStaffRepository.count();

        long orderCount = customerOrderRepository.countPlatformOrdersBetween(startAt, endAt);
        BigDecimal totalSales = customerOrderRepository.sumTotalSalesBetween(startAt, endAt);
        BigDecimal platformIncome = customerOrderRepository.sumPlatformCommissionBetween(startAt, endAt);

        List<StatusCountRow> statusCounts = customerOrderRepository.countPlatformByStatusBetween(startAt, endAt).stream()
                .map(p -> new StatusCountRow(p.getStatus(), p.getCount()))
                .toList();

        Pageable recentPage = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<RecentOrderRow> recentOrders = customerOrderRepository
                .findOrderList(null, null, null, null, startAt, endAt, recentPage)
                .getContent()
                .stream()
                .map(p -> new RecentOrderRow(
                        p.getId(),
                        p.getRestaurantName(),
                        p.getCustomerUsername(),
                        p.getStatus(),
                        p.getPayStatus(),
                        p.getPayAmount(),
                        p.getCreatedAt()
                ))
                .toList();

        DashboardResponse resp = new DashboardResponse(
                safeStart,
                safeEnd,
                restaurantCount,
                customerCount,
                deliveryStaffCount,
                orderCount,
                totalSales,
                platformIncome,
                statusCounts,
                recentOrders
        );
        return ResponseEntity.ok(resp);
    }

    public static class DashboardResponse {
        private LocalDate start;
        private LocalDate end;
        private Long restaurantCount;
        private Long customerCount;
        private Long deliveryStaffCount;
        private Long orderCount;
        private BigDecimal totalSales;
        private BigDecimal platformIncome;
        private List<StatusCountRow> statusCounts;
        private List<RecentOrderRow> recentOrders;

        public DashboardResponse(LocalDate start,
                                 LocalDate end,
                                 Long restaurantCount,
                                 Long customerCount,
                                 Long deliveryStaffCount,
                                 Long orderCount,
                                 BigDecimal totalSales,
                                 BigDecimal platformIncome,
                                 List<StatusCountRow> statusCounts,
                                 List<RecentOrderRow> recentOrders) {
            this.start = start;
            this.end = end;
            this.restaurantCount = restaurantCount;
            this.customerCount = customerCount;
            this.deliveryStaffCount = deliveryStaffCount;
            this.orderCount = orderCount;
            this.totalSales = totalSales;
            this.platformIncome = platformIncome;
            this.statusCounts = statusCounts;
            this.recentOrders = recentOrders;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public Long getRestaurantCount() {
            return restaurantCount;
        }

        public Long getCustomerCount() {
            return customerCount;
        }

        public Long getDeliveryStaffCount() {
            return deliveryStaffCount;
        }

        public Long getOrderCount() {
            return orderCount;
        }

        public BigDecimal getTotalSales() {
            return totalSales;
        }

        public BigDecimal getPlatformIncome() {
            return platformIncome;
        }

        public List<StatusCountRow> getStatusCounts() {
            return statusCounts;
        }

        public List<RecentOrderRow> getRecentOrders() {
            return recentOrders;
        }
    }

    public static class StatusCountRow {
        private String status;
        private Long count;

        public StatusCountRow(String status, Long count) {
            this.status = status;
            this.count = count;
        }

        public String getStatus() {
            return status;
        }

        public Long getCount() {
            return count;
        }
    }

    public static class RecentOrderRow {
        private Long id;
        private String restaurantName;
        private String customerUsername;
        private String status;
        private String payStatus;
        private BigDecimal payAmount;
        private LocalDateTime createdAt;

        public RecentOrderRow(Long id,
                              String restaurantName,
                              String customerUsername,
                              String status,
                              String payStatus,
                              BigDecimal payAmount,
                              LocalDateTime createdAt) {
            this.id = id;
            this.restaurantName = restaurantName;
            this.customerUsername = customerUsername;
            this.status = status;
            this.payStatus = payStatus;
            this.payAmount = payAmount;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getRestaurantName() {
            return restaurantName;
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
    }
}

