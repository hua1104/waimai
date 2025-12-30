package com.example.takeout.web;

import com.example.takeout.repository.DeliveryRatingRepository;
import com.example.takeout.repository.RestaurantRatingRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ratings")
public class AdminRatingController {

    private final RestaurantRatingRepository restaurantRatingRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;

    public AdminRatingController(RestaurantRatingRepository restaurantRatingRepository,
                                 DeliveryRatingRepository deliveryRatingRepository) {
        this.restaurantRatingRepository = restaurantRatingRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
    }

    @GetMapping("/restaurants")
    public List<RestaurantRatingRepository.RestaurantRatingListProjection> listRestaurantRatings(
            @RequestParam(value = "restaurantId", required = false) Long restaurantId,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start == null ? null : start.atStartOfDay();
        LocalDateTime endAt = end == null ? null : end.plusDays(1).atStartOfDay().minusNanos(1);
        return restaurantRatingRepository.findAdminList(restaurantId, customerId, startAt, endAt);
    }

    @DeleteMapping("/restaurants/{id}")
    @Transactional
    public ResponseEntity<Void> deleteRestaurantRating(@PathVariable("id") Long id) {
        if (!restaurantRatingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        restaurantRatingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delivery")
    public List<DeliveryRatingRepository.DeliveryRatingListProjection> listDeliveryRatings(
            @RequestParam(value = "deliveryStaffId", required = false) Long deliveryStaffId,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDateTime startAt = start == null ? null : start.atStartOfDay();
        LocalDateTime endAt = end == null ? null : end.plusDays(1).atStartOfDay().minusNanos(1);
        return deliveryRatingRepository.findAdminList(deliveryStaffId, customerId, startAt, endAt);
    }

    @DeleteMapping("/delivery/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDeliveryRating(@PathVariable("id") Long id) {
        if (!deliveryRatingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deliveryRatingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

