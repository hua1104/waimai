package com.example.takeout.repository;

import com.example.takeout.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long deleteByOrder_Restaurant_Id(Long restaurantId);

    List<OrderItem> findByOrder_Id(Long orderId);

    interface DishSalesProjection {
        Long getDishId();
        String getDishName();
        Long getQuantity();
        BigDecimal getSalesAmount();
    }

    @Query("""
            select d.id as dishId,
                   d.name as dishName,
                   coalesce(sum(oi.quantity), 0) as quantity,
                   coalesce(sum(oi.unitPrice * oi.quantity), 0) as salesAmount
            from OrderItem oi
            join oi.order o
            join oi.dish d
            where o.restaurant.id = :restaurantId
              and o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            group by d.id, d.name
            order by salesAmount desc
            """)
    List<DishSalesProjection> topDishesBetween(@Param("restaurantId") Long restaurantId,
                                              @Param("startAt") LocalDateTime startAt,
                                              @Param("endAt") LocalDateTime endAt);

    @Query("""
            select d.id as dishId,
                   d.name as dishName,
                   coalesce(sum(oi.quantity), 0) as quantity,
                   coalesce(sum(oi.unitPrice * oi.quantity), 0) as salesAmount
            from OrderItem oi
            join oi.order o
            join oi.dish d
            where o.customer.id = :customerId
              and o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            group by d.id, d.name
            order by salesAmount desc
            """)
    List<DishSalesProjection> topDishesForCustomerBetween(@Param("customerId") Long customerId,
                                                          @Param("startAt") LocalDateTime startAt,
                                                          @Param("endAt") LocalDateTime endAt);

    @Query("""
            select d.id as dishId,
                   d.name as dishName,
                   coalesce(sum(oi.quantity), 0) as quantity,
                   coalesce(sum(oi.unitPrice * oi.quantity), 0) as salesAmount
            from OrderItem oi
            join oi.order o
            join oi.dish d
            where o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            group by d.id, d.name
            order by salesAmount desc
            """)
    List<DishSalesProjection> topDishesPlatformBetween(@Param("startAt") LocalDateTime startAt,
                                                       @Param("endAt") LocalDateTime endAt);
}
