package com.example.takeout.repository;

import com.example.takeout.entity.RestaurantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantUserRepository extends JpaRepository<RestaurantUser, Long> {

    Optional<RestaurantUser> findByUsername(String username);

    Optional<RestaurantUser> findByUsernameAndPassword(String username, String password);

    boolean existsByUsername(String username);

    long deleteByRestaurant_Id(Long restaurantId);
}
