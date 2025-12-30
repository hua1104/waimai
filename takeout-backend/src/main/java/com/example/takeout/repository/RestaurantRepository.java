package com.example.takeout.repository;

import com.example.takeout.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByStatusNot(String status);

    @Query("select r from Restaurant r where r.status is null or r.status = 'ACTIVE'")
    List<Restaurant> findPublicActive();
}
