package com.example.takeout.repository;

import com.example.takeout.entity.PaymentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    interface PaymentLogListProjection {
        Long getId();

        Long getOrderId();

        String getType();

        BigDecimal getAmount();

        String getMethod();

        String getOperatorRole();

        Long getOperatorId();

        String getStatus();

        String getNote();

        LocalDateTime getCreatedAt();
    }

    @Query("""
            select pl.id as id,
                   o.id as orderId,
                   pl.type as type,
                   pl.amount as amount,
                   pl.method as method,
                   pl.operatorRole as operatorRole,
                   pl.operatorId as operatorId,
                   pl.status as status,
                   pl.note as note,
                   pl.createdAt as createdAt
            from PaymentLog pl
            join pl.order o
            where (:orderId is null or o.id = :orderId)
              and (:type is null or pl.type = :type)
              and (:startAt is null or pl.createdAt >= :startAt)
              and (:endAt is null or pl.createdAt <= :endAt)
            """)
    Page<PaymentLogListProjection> findLogs(@Param("orderId") Long orderId,
                                           @Param("type") String type,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt,
                                           Pageable pageable);
}

