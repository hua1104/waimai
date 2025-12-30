package com.example.takeout.repository;

import com.example.takeout.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    long deleteByRestaurant_Id(Long restaurantId);

    List<Dish> findByRestaurant_IdAndStatus(Long restaurantId, String status);

    List<Dish> findByRestaurant_Id(Long restaurantId);

    long countByCategory_Id(Long categoryId);
}
