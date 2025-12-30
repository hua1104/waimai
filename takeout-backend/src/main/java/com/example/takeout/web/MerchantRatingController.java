package com.example.takeout.web;

import com.example.takeout.repository.DeliveryRatingRepository;
import com.example.takeout.repository.RestaurantRatingRepository;
import com.example.takeout.repository.RestaurantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/merchant")
public class MerchantRatingController {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantRatingRepository restaurantRatingRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;

    public MerchantRatingController(RestaurantRepository restaurantRepository,
                                    RestaurantRatingRepository restaurantRatingRepository,
                                    DeliveryRatingRepository deliveryRatingRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantRatingRepository = restaurantRatingRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
    }

    @GetMapping("/{restaurantId}/ratings/summary")
    public ResponseEntity<SummaryResponse> summary(@PathVariable("restaurantId") Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build();
        }
        BigDecimal restaurantAvg = restaurantRatingRepository.avgScoreByRestaurant(restaurantId);
        long restaurantCount = restaurantRatingRepository.countByRestaurant_Id(restaurantId);
        BigDecimal deliveryAvg = deliveryRatingRepository.avgScoreByRestaurant(restaurantId);
        long deliveryCount = deliveryRatingRepository.countByRestaurant(restaurantId);

        return ResponseEntity.ok(new SummaryResponse(restaurantAvg, restaurantCount, deliveryAvg, deliveryCount));
    }

    public static class SummaryResponse {
        private BigDecimal restaurantAvgScore;
        private Long restaurantRatingCount;
        private BigDecimal deliveryAvgScore;
        private Long deliveryRatingCount;

        public SummaryResponse(BigDecimal restaurantAvgScore,
                               Long restaurantRatingCount,
                               BigDecimal deliveryAvgScore,
                               Long deliveryRatingCount) {
            this.restaurantAvgScore = restaurantAvgScore;
            this.restaurantRatingCount = restaurantRatingCount;
            this.deliveryAvgScore = deliveryAvgScore;
            this.deliveryRatingCount = deliveryRatingCount;
        }

        public BigDecimal getRestaurantAvgScore() {
            return restaurantAvgScore;
        }

        public Long getRestaurantRatingCount() {
            return restaurantRatingCount;
        }

        public BigDecimal getDeliveryAvgScore() {
            return deliveryAvgScore;
        }

        public Long getDeliveryRatingCount() {
            return deliveryRatingCount;
        }
    }
}

