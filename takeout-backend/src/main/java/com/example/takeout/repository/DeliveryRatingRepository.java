package com.example.takeout.repository;

import com.example.takeout.entity.DeliveryRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryRatingRepository extends JpaRepository<DeliveryRating, Long> {

    boolean existsByOrder_Id(Long orderId);

    Optional<DeliveryRating> findByOrder_Id(Long orderId);

    long deleteByOrder_Restaurant_Id(Long restaurantId);

    @Query("select count(r) from DeliveryRating r join r.order o where o.restaurant.id = :restaurantId")
    long countByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("select coalesce(avg(r.score), 0) from DeliveryRating r join r.order o where o.restaurant.id = :restaurantId")
    BigDecimal avgScoreByRestaurant(@Param("restaurantId") Long restaurantId);

    interface RiderRatingRankProjection {
        Long getDeliveryStaffId();

        String getDeliveryStaffName();

        BigDecimal getAvgScore();

        Long getRatingCount();
    }

    @Query("""
            select r.deliveryStaff.id as deliveryStaffId,
                   r.deliveryStaff.name as deliveryStaffName,
                   coalesce(avg(r.score), 0) as avgScore,
                   count(r) as ratingCount
            from DeliveryRating r
            where r.createdAt between :startAt and :endAt
            group by r.deliveryStaff.id, r.deliveryStaff.name
            order by avgScore desc, ratingCount desc
            """)
    List<RiderRatingRankProjection> rankRidersBetween(@Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt);

    interface DeliveryRatingListProjection {
        Long getId();

        Long getOrderId();

        Long getCustomerId();

        String getCustomerUsername();

        Long getDeliveryStaffId();

        String getDeliveryStaffName();

        Integer getScore();

        String getContent();

        LocalDateTime getCreatedAt();
    }

    @Query("""
            select dr.id as id,
                   o.id as orderId,
                   c.id as customerId,
                   c.username as customerUsername,
                   ds.id as deliveryStaffId,
                   ds.name as deliveryStaffName,
                   dr.score as score,
                   dr.content as content,
                   dr.createdAt as createdAt
            from DeliveryRating dr
            join dr.order o
            join dr.customer c
            join dr.deliveryStaff ds
            where (:deliveryStaffId is null or ds.id = :deliveryStaffId)
              and (:customerId is null or c.id = :customerId)
              and (:startAt is null or dr.createdAt >= :startAt)
              and (:endAt is null or dr.createdAt <= :endAt)
            order by dr.createdAt desc
            """)
    List<DeliveryRatingListProjection> findAdminList(@Param("deliveryStaffId") Long deliveryStaffId,
                                                     @Param("customerId") Long customerId,
                                                     @Param("startAt") LocalDateTime startAt,
                                                     @Param("endAt") LocalDateTime endAt);
}
