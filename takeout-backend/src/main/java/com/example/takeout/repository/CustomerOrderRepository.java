package com.example.takeout.repository;

import com.example.takeout.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findFirstByPayOutTradeNo(String payOutTradeNo);

    @Query("""
            select coalesce(sum(o.payAmount), 0)
            from CustomerOrder o
            where o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            """)
    BigDecimal sumTotalSalesBetween(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("""
            select coalesce(sum(o.commissionAmount), 0)
            from CustomerOrder o
            where o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            """)
    BigDecimal sumPlatformCommissionBetween(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    interface RestaurantSalesProjection {
        Long getRestaurantId();
        String getRestaurantName();
        BigDecimal getSalesAmount();
    }

    @Query("""
            select r.id as restaurantId,
                   r.name as restaurantName,
                   coalesce(sum(o.payAmount), 0) as salesAmount
            from CustomerOrder o
            join o.restaurant r
            where o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            group by r.id, r.name
            order by salesAmount desc
            """)
    List<RestaurantSalesProjection> topRestaurantsBetween(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    long deleteByRestaurant_Id(Long restaurantId);

    interface OrderListProjection {
        Long getId();
        Long getRestaurantId();
        String getRestaurantName();
        Long getCustomerId();
        String getCustomerUsername();
        String getStatus();
        String getPayStatus();
        BigDecimal getPayAmount();
        LocalDateTime getCreatedAt();
        LocalDateTime getPaidAt();
    }

    @Query("""
            select o.id as id,
                   r.id as restaurantId,
                   r.name as restaurantName,
                   c.id as customerId,
                   c.username as customerUsername,
                   o.status as status,
                   o.payStatus as payStatus,
                   o.payAmount as payAmount,
                   o.createdAt as createdAt,
                   o.paidAt as paidAt
            from CustomerOrder o
            join o.restaurant r
            join o.customer c
            where (:status is null or o.status = :status)
              and (:payStatus is null or o.payStatus = :payStatus)
              and (:restaurantId is null or r.id = :restaurantId)
              and (:customerId is null or c.id = :customerId)
              and (:startAt is null or o.createdAt >= :startAt)
              and (:endAt is null or o.createdAt <= :endAt)
            """)
    Page<OrderListProjection> findOrderList(@Param("status") String status,
                                           @Param("payStatus") String payStatus,
                                           @Param("restaurantId") Long restaurantId,
                                           @Param("customerId") Long customerId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt,
                                           Pageable pageable);

    @Query("""
            select coalesce(sum(o.payAmount), 0)
            from CustomerOrder o
            where o.restaurant.id = :restaurantId
              and o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            """)
    BigDecimal sumRestaurantSalesBetween(@Param("restaurantId") Long restaurantId,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt);

    @Query("""
            select coalesce(sum(o.payAmount), 0)
            from CustomerOrder o
            where o.customer.id = :customerId
              and o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            """)
    BigDecimal sumCustomerSpendBetween(@Param("customerId") Long customerId,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);

    @Query("""
            select count(o)
            from CustomerOrder o
            where o.customer.id = :customerId
              and o.createdAt between :startAt and :endAt
            """)
    long countCustomerOrdersBetween(@Param("customerId") Long customerId,
                                    @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt);

    interface StatusCountProjection {
        String getStatus();

        Long getCount();
    }

    @Query("""
            select o.status as status, count(o) as count
            from CustomerOrder o
            where o.customer.id = :customerId
              and o.createdAt between :startAt and :endAt
            group by o.status
            """)
    List<StatusCountProjection> countCustomerByStatusBetween(@Param("customerId") Long customerId,
                                                             @Param("startAt") LocalDateTime startAt,
                                                             @Param("endAt") LocalDateTime endAt);

    interface PlatformStatusCountProjection {
        String getStatus();

        Long getCount();
    }

    @Query("""
            select o.status as status, count(o) as count
            from CustomerOrder o
            where (:startAt is null or o.createdAt >= :startAt)
              and (:endAt is null or o.createdAt <= :endAt)
            group by o.status
            """)
    List<PlatformStatusCountProjection> countPlatformByStatusBetween(@Param("startAt") LocalDateTime startAt,
                                                                     @Param("endAt") LocalDateTime endAt);

    @Query("""
            select count(o)
            from CustomerOrder o
            where (:startAt is null or o.createdAt >= :startAt)
              and (:endAt is null or o.createdAt <= :endAt)
            """)
    long countPlatformOrdersBetween(@Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    @Query("""
            select r.id as restaurantId,
                   r.name as restaurantName,
                   coalesce(sum(o.payAmount), 0) as salesAmount
            from CustomerOrder o
            join o.restaurant r
            where o.customer.id = :customerId
              and o.paidAt is not null
              and o.payStatus = 'PAID'
              and o.paidAt between :startAt and :endAt
            group by r.id, r.name
            order by salesAmount desc
            """)
    List<RestaurantSalesProjection> topRestaurantsForCustomerBetween(@Param("customerId") Long customerId,
                                                                     @Param("startAt") LocalDateTime startAt,
                                                                     @Param("endAt") LocalDateTime endAt);

    interface RiderOrderListProjection {
        Long getId();

        Long getRestaurantId();

        String getRestaurantName();

        Long getCustomerId();

        String getCustomerUsername();

        Long getDeliveryStaffId();

        String getStatus();

        String getPayStatus();

        BigDecimal getPayAmount();

        LocalDateTime getCreatedAt();

        LocalDateTime getPaidAt();

        String getAddressDetail();

        String getContactName();

        String getContactPhone();
    }

    @Query("""
            select o.id as id,
                   r.id as restaurantId,
                   r.name as restaurantName,
                   c.id as customerId,
                   c.username as customerUsername,
                   ds.id as deliveryStaffId,
                   o.status as status,
                   o.payStatus as payStatus,
                   o.payAmount as payAmount,
                   o.createdAt as createdAt,
                   o.paidAt as paidAt,
                   o.addressDetail as addressDetail,
                   o.contactName as contactName,
                   o.contactPhone as contactPhone
            from CustomerOrder o
            join o.restaurant r
            join o.customer c
            join o.deliveryStaff ds
            where ds.id = :deliveryStaffId
              and (:status is null or o.status = :status)
              and (:startAt is null or o.createdAt >= :startAt)
              and (:endAt is null or o.createdAt <= :endAt)
            """)
    Page<RiderOrderListProjection> findRiderOrderList(@Param("deliveryStaffId") Long deliveryStaffId,
                                                      @Param("status") String status,
                                                      @Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt,
                                                      Pageable pageable);

    interface RiderHallOrderProjection {
        Long getId();

        Long getRestaurantId();

        String getRestaurantName();

        Long getCustomerId();

        String getCustomerUsername();

        String getStatus();

        String getPayStatus();

        BigDecimal getPayAmount();

        LocalDateTime getCreatedAt();

        LocalDateTime getPaidAt();

        String getAddressDetail();

        String getContactName();

        String getContactPhone();
    }

    @Query("""
            select o.id as id,
                   r.id as restaurantId,
                   r.name as restaurantName,
                   c.id as customerId,
                   c.username as customerUsername,
                   o.status as status,
                   o.payStatus as payStatus,
                   o.payAmount as payAmount,
                   o.createdAt as createdAt,
                   o.paidAt as paidAt,
                   o.addressDetail as addressDetail,
                   o.contactName as contactName,
                   o.contactPhone as contactPhone
            from CustomerOrder o
            join o.restaurant r
            join o.customer c
            where o.deliveryStaff is null
              and o.status = 'PAID'
              and o.payStatus = 'PAID'
              and (:startAt is null or o.createdAt >= :startAt)
              and (:endAt is null or o.createdAt <= :endAt)
            """)
    Page<RiderHallOrderProjection> findRiderHallOrders(@Param("startAt") LocalDateTime startAt,
                                                       @Param("endAt") LocalDateTime endAt,
                                                       Pageable pageable);

    @Query("""
            select count(o)
            from CustomerOrder o
            join o.deliveryStaff ds
            where ds.id = :deliveryStaffId
              and o.status = 'DELIVERING'
            """)
    long countRiderDelivering(@Param("deliveryStaffId") Long deliveryStaffId);

    @Query("""
            select count(o)
            from CustomerOrder o
            join o.deliveryStaff ds
            where ds.id = :deliveryStaffId
              and o.status = 'COMPLETED'
              and o.finishedAt is not null
              and o.finishedAt between :startAt and :endAt
            """)
    long countRiderCompletedBetween(@Param("deliveryStaffId") Long deliveryStaffId,
                                    @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt);

    @Query("""
            select o
            from CustomerOrder o
            where o.payStatus = 'UNPAID'
              and o.status = 'CREATED'
              and o.createdAt <= :deadline
            """)
    List<CustomerOrder> findStaleUnpaidOrders(@Param("deadline") LocalDateTime deadline);

    @Query("""
            select o
            from CustomerOrder o
            where o.payStatus = 'PAID'
              and o.status = 'PAID'
              and o.deliveryStaff is null
              and o.paidAt is not null
              and o.paidAt <= :deadline
            """)
    List<CustomerOrder> findStalePaidUnassignedOrders(@Param("deadline") LocalDateTime deadline);
}
