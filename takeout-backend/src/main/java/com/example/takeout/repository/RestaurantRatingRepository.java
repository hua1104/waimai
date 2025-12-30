package com.example.takeout.repository;

import com.example.takeout.entity.RestaurantRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantRatingRepository extends JpaRepository<RestaurantRating, Long> {

    boolean existsByOrder_Id(Long orderId);

    Optional<RestaurantRating> findByOrder_Id(Long orderId);

    long deleteByRestaurant_Id(Long restaurantId);

    long countByRestaurant_Id(Long restaurantId);

    @Query("select coalesce(avg(r.score), 0) from RestaurantRating r where r.restaurant.id = :restaurantId")
    BigDecimal avgScoreByRestaurant(@Param("restaurantId") Long restaurantId);

    interface RestaurantRatingRankProjection {
        Long getRestaurantId();

        String getRestaurantName();

        BigDecimal getAvgScore();

        Long getRatingCount();
    }

    @Query("""
            select r.restaurant.id as restaurantId,
                   r.restaurant.name as restaurantName,
                   coalesce(avg(r.score), 0) as avgScore,
                   count(r) as ratingCount
            from RestaurantRating r
            where r.createdAt between :startAt and :endAt
            group by r.restaurant.id, r.restaurant.name
            order by avgScore desc, ratingCount desc
            """)
    List<RestaurantRatingRankProjection> rankRestaurantsBetween(@Param("startAt") LocalDateTime startAt,
                                                                @Param("endAt") LocalDateTime endAt);

    interface RestaurantRatingListProjection {
        Long getId();

        Long getOrderId();

        Long getCustomerId();

        String getCustomerUsername();

        Long getRestaurantId();

        String getRestaurantName();

        Integer getScore();

        String getContent();

        LocalDateTime getCreatedAt();
    }

    @Query("""
            select rr.id as id,
                   o.id as orderId,
                   c.id as customerId,
                   c.username as customerUsername,
                   r.id as restaurantId,
                   r.name as restaurantName,
                   rr.score as score,
                   rr.content as content,
                   rr.createdAt as createdAt
            from RestaurantRating rr
            join rr.order o
            join rr.customer c
            join rr.restaurant r
            where (:restaurantId is null or r.id = :restaurantId)
              and (:customerId is null or c.id = :customerId)
              and (:startAt is null or rr.createdAt >= :startAt)
              and (:endAt is null or rr.createdAt <= :endAt)
            order by rr.createdAt desc
            """)
    List<RestaurantRatingListProjection> findAdminList(@Param("restaurantId") Long restaurantId,
                                                       @Param("customerId") Long customerId,
                                                       @Param("startAt") LocalDateTime startAt,
                                                       @Param("endAt") LocalDateTime endAt);
}
