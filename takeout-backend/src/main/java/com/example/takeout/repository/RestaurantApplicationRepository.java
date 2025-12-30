package com.example.takeout.repository;

import com.example.takeout.entity.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantApplicationRepository extends JpaRepository<RestaurantApplication, Long> {

    boolean existsByUsername(String username);

    Optional<RestaurantApplication> findTopByUsernameOrderByCreatedAtDesc(String username);

    @Query("""
            select a
            from RestaurantApplication a
            where (:status is null or a.status = :status)
            order by a.createdAt desc
            """)
    List<RestaurantApplication> findAdminList(@Param("status") String status);
}

