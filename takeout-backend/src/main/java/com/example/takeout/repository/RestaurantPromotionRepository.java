package com.example.takeout.repository;

import com.example.takeout.entity.RestaurantPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RestaurantPromotionRepository extends JpaRepository<RestaurantPromotion, Long> {

    List<RestaurantPromotion> findByRestaurant_IdOrderByCreatedAtDesc(Long restaurantId);

    long deleteByRestaurant_Id(Long restaurantId);

    @Query("""
            select p
            from RestaurantPromotion p
            where p.restaurant.id = :restaurantId
              and p.status = 'ACTIVE'
              and (:now is null or (coalesce(p.startAt, :now) <= :now and coalesce(p.endAt, :now) >= :now))
            order by p.thresholdAmount desc, p.discountAmount desc, p.id desc
            """)
    List<RestaurantPromotion> findActiveAt(@Param("restaurantId") Long restaurantId,
                                          @Param("now") LocalDateTime now);
}

