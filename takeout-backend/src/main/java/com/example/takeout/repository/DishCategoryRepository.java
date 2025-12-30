package com.example.takeout.repository;

import com.example.takeout.entity.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishCategoryRepository extends JpaRepository<DishCategory, Long> {

    long deleteByRestaurant_Id(Long restaurantId);

    List<DishCategory> findByRestaurant_Id(Long restaurantId);
}
