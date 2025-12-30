package com.example.takeout.web;

import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.DeliveryRatingRepository;
import com.example.takeout.repository.OrderItemRepository;
import com.example.takeout.repository.RestaurantRatingRepository;
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
@RequestMapping("/api/stats")
public class StatsController {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRatingRepository restaurantRatingRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;

    public StatsController(CustomerOrderRepository customerOrderRepository,
                           OrderItemRepository orderItemRepository,
                           RestaurantRatingRepository restaurantRatingRepository,
                           DeliveryRatingRepository deliveryRatingRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRatingRepository = restaurantRatingRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
    }

    @GetMapping("/platform")
    public ResponseEntity<PlatformStatsResponse> platform(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);

        BigDecimal totalSales = customerOrderRepository.sumTotalSalesBetween(startAt, endAt);
        BigDecimal platformIncome = customerOrderRepository.sumPlatformCommissionBetween(startAt, endAt);
        List<RestaurantSalesRow> topRestaurants = customerOrderRepository.topRestaurantsBetween(startAt, endAt)
                .stream()
                .map(r -> new RestaurantSalesRow(r.getRestaurantId(), r.getRestaurantName(), r.getSalesAmount()))
                .toList();

        PlatformStatsResponse resp = new PlatformStatsResponse(totalSales, platformIncome, topRestaurants);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/dishes")
    public ResponseEntity<TopDishesResponse> dishes(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);

        List<TopDishRow> topDishes = orderItemRepository.topDishesPlatformBetween(startAt, endAt).stream()
                .limit(20)
                .map(p -> new TopDishRow(p.getDishId(), p.getDishName(), p.getQuantity(), p.getSalesAmount()))
                .toList();

        return ResponseEntity.ok(new TopDishesResponse(topDishes));
    }

    @GetMapping("/ratings")
    public ResponseEntity<RatingsRankResponse> ratings(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);

        List<RestaurantRatingRankRow> restaurantRanks = restaurantRatingRepository.rankRestaurantsBetween(startAt, endAt).stream()
                .limit(20)
                .map(p -> new RestaurantRatingRankRow(p.getRestaurantId(), p.getRestaurantName(), p.getAvgScore(), p.getRatingCount()))
                .toList();

        List<RiderRatingRankRow> riderRanks = deliveryRatingRepository.rankRidersBetween(startAt, endAt).stream()
                .limit(20)
                .map(p -> new RiderRatingRankRow(p.getDeliveryStaffId(), p.getDeliveryStaffName(), p.getAvgScore(), p.getRatingCount()))
                .toList();

        return ResponseEntity.ok(new RatingsRankResponse(restaurantRanks, riderRanks));
    }

    public static class PlatformStatsResponse {
        private BigDecimal totalSales;
        private BigDecimal platformIncome;
        private List<RestaurantSalesRow> topRestaurants;

        public PlatformStatsResponse(BigDecimal totalSales, BigDecimal platformIncome, List<RestaurantSalesRow> topRestaurants) {
            this.totalSales = totalSales;
            this.platformIncome = platformIncome;
            this.topRestaurants = topRestaurants;
        }

        public BigDecimal getTotalSales() {
            return totalSales;
        }

        public BigDecimal getPlatformIncome() {
            return platformIncome;
        }

        public List<RestaurantSalesRow> getTopRestaurants() {
            return topRestaurants;
        }
    }

    public static class RestaurantSalesRow {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal salesAmount;

        public RestaurantSalesRow(Long restaurantId, String restaurantName, BigDecimal salesAmount) {
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

    public static class TopDishesResponse {
        private List<TopDishRow> topDishes;

        public TopDishesResponse(List<TopDishRow> topDishes) {
            this.topDishes = topDishes;
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

    public static class RatingsRankResponse {
        private List<RestaurantRatingRankRow> topRestaurantsByRating;
        private List<RiderRatingRankRow> topRidersByRating;

        public RatingsRankResponse(List<RestaurantRatingRankRow> topRestaurantsByRating,
                                   List<RiderRatingRankRow> topRidersByRating) {
            this.topRestaurantsByRating = topRestaurantsByRating;
            this.topRidersByRating = topRidersByRating;
        }

        public List<RestaurantRatingRankRow> getTopRestaurantsByRating() {
            return topRestaurantsByRating;
        }

        public List<RiderRatingRankRow> getTopRidersByRating() {
            return topRidersByRating;
        }
    }

    public static class RestaurantRatingRankRow {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal avgScore;
        private Long ratingCount;

        public RestaurantRatingRankRow(Long restaurantId, String restaurantName, BigDecimal avgScore, Long ratingCount) {
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.avgScore = avgScore;
            this.ratingCount = ratingCount;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public BigDecimal getAvgScore() {
            return avgScore;
        }

        public Long getRatingCount() {
            return ratingCount;
        }
    }

    public static class RiderRatingRankRow {
        private Long deliveryStaffId;
        private String deliveryStaffName;
        private BigDecimal avgScore;
        private Long ratingCount;

        public RiderRatingRankRow(Long deliveryStaffId, String deliveryStaffName, BigDecimal avgScore, Long ratingCount) {
            this.deliveryStaffId = deliveryStaffId;
            this.deliveryStaffName = deliveryStaffName;
            this.avgScore = avgScore;
            this.ratingCount = ratingCount;
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public String getDeliveryStaffName() {
            return deliveryStaffName;
        }

        public BigDecimal getAvgScore() {
            return avgScore;
        }

        public Long getRatingCount() {
            return ratingCount;
        }
    }
}
