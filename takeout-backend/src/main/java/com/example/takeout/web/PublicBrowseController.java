package com.example.takeout.web;

import com.example.takeout.entity.Dish;
import com.example.takeout.entity.Restaurant;
import com.example.takeout.repository.DishRepository;
import com.example.takeout.repository.RestaurantPromotionRepository;
import com.example.takeout.repository.RestaurantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicBrowseController {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final RestaurantPromotionRepository restaurantPromotionRepository;

    public PublicBrowseController(RestaurantRepository restaurantRepository,
                                  DishRepository dishRepository,
                                  RestaurantPromotionRepository restaurantPromotionRepository) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.restaurantPromotionRepository = restaurantPromotionRepository;
    }

    @GetMapping("/restaurants")
    public List<Restaurant> restaurants() {
        return restaurantRepository.findPublicActive();
    }

    @GetMapping("/restaurants/{id}/dishes")
    public ResponseEntity<List<DishRow>> dishes(@PathVariable("id") Long restaurantId) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 仅返回上架菜品；返回 DTO 避免 Hibernate 懒加载代理序列化报错
        List<Dish> dishes = dishRepository.findByRestaurant_IdAndStatus(restaurantId, "AVAILABLE");
        List<DishRow> rows = dishes.stream()
                .map(d -> new DishRow(d.getId(), d.getName(), d.getPrice(), d.getStatus(), d.getDiscountType(), d.getDiscountValue()))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/restaurants/{id}/promotions")
    public ResponseEntity<List<PromotionRow>> promotions(@PathVariable("id") Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build();
        }
        LocalDateTime now = LocalDateTime.now();
        List<PromotionRow> rows = restaurantPromotionRepository.findActiveAt(restaurantId, now).stream()
                .filter(p -> "FULL_REDUCTION".equalsIgnoreCase(p.getType()))
                .map(p -> new PromotionRow(p.getId(), p.getType(), p.getThresholdAmount(), p.getDiscountAmount()))
                .toList();
        return ResponseEntity.ok(rows);
    }

    public static class DishRow {
        private Long id;
        private String name;
        private BigDecimal price;
        private String status;
        private String discountType;
        private BigDecimal discountValue;

        public DishRow(Long id, String name, BigDecimal price, String status, String discountType, BigDecimal discountValue) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.status = status;
            this.discountType = discountType;
            this.discountValue = discountValue;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getStatus() {
            return status;
        }

        public String getDiscountType() {
            return discountType;
        }

        public BigDecimal getDiscountValue() {
            return discountValue;
        }
    }

    public static class PromotionRow {
        private Long id;
        private String type;
        private BigDecimal thresholdAmount;
        private BigDecimal discountAmount;

        public PromotionRow(Long id, String type, BigDecimal thresholdAmount, BigDecimal discountAmount) {
            this.id = id;
            this.type = type;
            this.thresholdAmount = thresholdAmount;
            this.discountAmount = discountAmount;
        }

        public Long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public BigDecimal getThresholdAmount() {
            return thresholdAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }
    }
}
