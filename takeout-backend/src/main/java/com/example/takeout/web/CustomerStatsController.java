package com.example.takeout.web;

import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.CustomerRepository;
import com.example.takeout.repository.OrderItemRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/stats")
public class CustomerStatsController {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public CustomerStatsController(CustomerRepository customerRepository,
                                   CustomerOrderRepository customerOrderRepository,
                                   OrderItemRepository orderItemRepository) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestParam("customerId") Long customerId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (customerId == null || start == null || end == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "参数不完整"));
        }
        if (!customerRepository.existsById(customerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "食客不存在"));
        }

        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);

        BigDecimal spend = customerOrderRepository.sumCustomerSpendBetween(customerId, startAt, endAt);
        long totalOrders = customerOrderRepository.countCustomerOrdersBetween(customerId, startAt, endAt);

        List<CustomerOrderRepository.StatusCountProjection> statusCounts =
                customerOrderRepository.countCustomerByStatusBetween(customerId, startAt, endAt);
        List<StatusCountRow> statusRows = statusCounts.stream()
                .map(p -> new StatusCountRow(p.getStatus(), p.getCount()))
                .toList();

        List<CustomerOrderRepository.RestaurantSalesProjection> topRestaurants =
                customerOrderRepository.topRestaurantsForCustomerBetween(customerId, startAt, endAt);
        List<TopRestaurantRow> topRestaurantRows = topRestaurants.stream()
                .limit(10)
                .map(p -> new TopRestaurantRow(p.getRestaurantId(), p.getRestaurantName(), p.getSalesAmount()))
                .toList();

        List<OrderItemRepository.DishSalesProjection> topDishes =
                orderItemRepository.topDishesForCustomerBetween(customerId, startAt, endAt);
        List<TopDishRow> topDishRows = topDishes.stream()
                .limit(10)
                .map(p -> new TopDishRow(p.getDishId(), p.getDishName(), p.getQuantity(), p.getSalesAmount()))
                .toList();

        return ResponseEntity.ok(new SummaryResponse(spend, totalOrders, statusRows, topRestaurantRows, topDishRows));
    }

    public static class SummaryResponse {
        private BigDecimal totalSpend;
        private Long totalOrders;
        private List<StatusCountRow> statusCounts;
        private List<TopRestaurantRow> topRestaurants;
        private List<TopDishRow> topDishes;

        public SummaryResponse(BigDecimal totalSpend,
                               Long totalOrders,
                               List<StatusCountRow> statusCounts,
                               List<TopRestaurantRow> topRestaurants,
                               List<TopDishRow> topDishes) {
            this.totalSpend = totalSpend;
            this.totalOrders = totalOrders;
            this.statusCounts = statusCounts;
            this.topRestaurants = topRestaurants;
            this.topDishes = topDishes;
        }

        public BigDecimal getTotalSpend() {
            return totalSpend;
        }

        public Long getTotalOrders() {
            return totalOrders;
        }

        public List<StatusCountRow> getStatusCounts() {
            return statusCounts;
        }

        public List<TopRestaurantRow> getTopRestaurants() {
            return topRestaurants;
        }

        public List<TopDishRow> getTopDishes() {
            return topDishes;
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

    public static class TopRestaurantRow {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal salesAmount;

        public TopRestaurantRow(Long restaurantId, String restaurantName, BigDecimal salesAmount) {
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.salesAmount = salesAmount;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public BigDecimal getSalesAmount() {
            return salesAmount;
        }
    }

    public static class TopDishRow {
        private Long dishId;
        private String dishName;
        private Long quantity;
        private BigDecimal salesAmount;

        public TopDishRow(Long dishId, String dishName, Long quantity, BigDecimal salesAmount) {
            this.dishId = dishId;
            this.dishName = dishName;
            this.quantity = quantity;
            this.salesAmount = salesAmount;
        }

        public Long getDishId() {
            return dishId;
        }

        public String getDishName() {
            return dishName;
        }

        public Long getQuantity() {
            return quantity;
        }

        public BigDecimal getSalesAmount() {
            return salesAmount;
        }
    }
}

