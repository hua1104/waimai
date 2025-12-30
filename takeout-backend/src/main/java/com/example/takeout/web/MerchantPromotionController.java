package com.example.takeout.web;

import com.example.takeout.entity.Restaurant;
import com.example.takeout.entity.RestaurantPromotion;
import com.example.takeout.repository.RestaurantPromotionRepository;
import com.example.takeout.repository.RestaurantRepository;
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
@RequestMapping("/api/merchant")
public class MerchantPromotionController {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantPromotionRepository promotionRepository;

    public MerchantPromotionController(RestaurantRepository restaurantRepository,
                                       RestaurantPromotionRepository promotionRepository) {
        this.restaurantRepository = restaurantRepository;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping("/{restaurantId}/promotions")
    public ResponseEntity<List<PromotionRow>> list(@PathVariable("restaurantId") Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) return ResponseEntity.notFound().build();
        List<PromotionRow> rows = promotionRepository.findByRestaurant_IdOrderByCreatedAtDesc(restaurantId).stream()
                .map(MerchantPromotionController::toRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{restaurantId}/promotions")
    public ResponseEntity<?> create(@PathVariable("restaurantId") Long restaurantId, @RequestBody PromotionPayload payload) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (restaurantOpt.isEmpty()) return ResponseEntity.notFound().build();

        String err = validate(payload);
        if (err != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));

        RestaurantPromotion p = new RestaurantPromotion();
        p.setRestaurant(restaurantOpt.get());
        apply(p, payload);
        RestaurantPromotion saved = promotionRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRow(saved));
    }

    @PutMapping("/{restaurantId}/promotions/{id}")
    public ResponseEntity<?> update(@PathVariable("restaurantId") Long restaurantId,
                                    @PathVariable("id") Long id,
                                    @RequestBody PromotionPayload payload) {
        Optional<RestaurantPromotion> opt = promotionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantPromotion p = opt.get();
        if (p.getRestaurant() == null || p.getRestaurant().getId() == null || !p.getRestaurant().getId().equals(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String err = validate(payload);
        if (err != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));

        apply(p, payload);
        RestaurantPromotion saved = promotionRepository.save(p);
        return ResponseEntity.ok(toRow(saved));
    }

    @DeleteMapping("/{restaurantId}/promotions/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable("restaurantId") Long restaurantId,
                                       @PathVariable("id") Long id) {
        Optional<RestaurantPromotion> opt = promotionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantPromotion p = opt.get();
        if (p.getRestaurant() == null || p.getRestaurant().getId() == null || !p.getRestaurant().getId().equals(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        promotionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static String validate(PromotionPayload payload) {
        if (payload == null) return "参数不完整";
        if (payload.getThresholdAmount() == null || payload.getDiscountAmount() == null) return "满减金额不能为空";
        if (payload.getThresholdAmount().compareTo(BigDecimal.ZERO) <= 0) return "门槛金额必须大于0";
        if (payload.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) return "减免金额必须大于0";
        if (payload.getDiscountAmount().compareTo(payload.getThresholdAmount()) > 0) return "减免金额不能大于门槛金额";
        if (payload.getStart() != null && payload.getEnd() != null && payload.getEnd().isBefore(payload.getStart())) {
            return "结束日期不能早于开始日期";
        }
        if (payload.getStatus() != null && !payload.getStatus().isBlank()) {
            String s = payload.getStatus().trim().toUpperCase();
            if (!"ACTIVE".equals(s) && !"DISABLED".equals(s)) return "状态不合法";
        }
        return null;
    }

    private static void apply(RestaurantPromotion p, PromotionPayload payload) {
        p.setType("FULL_REDUCTION");
        p.setThresholdAmount(payload.getThresholdAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setDiscountAmount(payload.getDiscountAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setStatus(payload.getStatus() == null || payload.getStatus().isBlank() ? "ACTIVE" : payload.getStatus());
        p.setStartAt(payload.getStart() == null ? null : payload.getStart().atStartOfDay());
        p.setEndAt(payload.getEnd() == null ? null : payload.getEnd().plusDays(1).atStartOfDay().minusNanos(1));
    }

    private static PromotionRow toRow(RestaurantPromotion p) {
        return new PromotionRow(
                p.getId(),
                p.getType(),
                p.getThresholdAmount(),
                p.getDiscountAmount(),
                p.getStatus(),
                p.getStartAt() == null ? null : p.getStartAt().toString(),
                p.getEndAt() == null ? null : p.getEndAt().toString(),
                p.getCreatedAt() == null ? null : p.getCreatedAt().toString()
        );
    }

    public static class PromotionPayload {
        private BigDecimal thresholdAmount;
        private BigDecimal discountAmount;
        private String status;
        private LocalDate start;
        private LocalDate end;

        public PromotionPayload() {
        }

        public BigDecimal getThresholdAmount() {
            return thresholdAmount;
        }

        public void setThresholdAmount(BigDecimal thresholdAmount) {
            this.thresholdAmount = thresholdAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDate getStart() {
            return start;
        }

        public void setStart(LocalDate start) {
            this.start = start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public void setEnd(LocalDate end) {
            this.end = end;
        }
    }

    public static class PromotionRow {
        private Long id;
        private String type;
        private BigDecimal thresholdAmount;
        private BigDecimal discountAmount;
        private String status;
        private String startAt;
        private String endAt;
        private String createdAt;

        public PromotionRow(Long id,
                            String type,
                            BigDecimal thresholdAmount,
                            BigDecimal discountAmount,
                            String status,
                            String startAt,
                            String endAt,
                            String createdAt) {
            this.id = id;
            this.type = type;
            this.thresholdAmount = thresholdAmount;
            this.discountAmount = discountAmount;
            this.status = status;
            this.startAt = startAt;
            this.endAt = endAt;
            this.createdAt = createdAt;
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

        public String getStatus() {
            return status;
        }

        public String getStartAt() {
            return startAt;
        }

        public String getEndAt() {
            return endAt;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
