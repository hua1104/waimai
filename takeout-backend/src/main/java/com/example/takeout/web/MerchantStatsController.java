package com.example.takeout.web;

import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.OrderItemRepository;
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
@RequestMapping("/api/merchant/stats")
public class MerchantStatsController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public MerchantStatsController(CustomerOrderRepository customerOrderRepository, OrderItemRepository orderItemRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/sales")
    public ResponseEntity<MerchantSalesResponse> sales(
            @RequestParam("restaurantId") Long restaurantId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);

        BigDecimal sales = customerOrderRepository.sumRestaurantSalesBetween(restaurantId, startAt, endAt);
        List<OrderItemRepository.DishSalesProjection> top = orderItemRepository.topDishesBetween(restaurantId, startAt, endAt);
        List<TopDishRow> rows = top.stream()
                .map(p -> new TopDishRow(p.getDishId(), p.getDishName(), p.getQuantity(), p.getSalesAmount()))
                .toList();

        return ResponseEntity.ok(new MerchantSalesResponse(sales, rows));
    }

    public static class MerchantSalesResponse {
        private BigDecimal salesAmount;
        private List<TopDishRow> topDishes;

        public MerchantSalesResponse(BigDecimal salesAmount, List<TopDishRow> topDishes) {
            this.salesAmount = salesAmount;
            this.topDishes = topDishes;
        }

        public BigDecimal getSalesAmount() {
            return salesAmount;
        }

        public List<TopDishRow> getTopDishes() {
            return topDishes;
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

