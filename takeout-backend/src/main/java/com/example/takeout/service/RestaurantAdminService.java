package com.example.takeout.service;

import com.example.takeout.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantAdminService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final DishRepository dishRepository;
    private final DishCategoryRepository dishCategoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;
    private final RestaurantRatingRepository restaurantRatingRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public RestaurantAdminService(
            RestaurantRepository restaurantRepository,
            RestaurantUserRepository restaurantUserRepository,
            DishRepository dishRepository,
            DishCategoryRepository dishCategoryRepository,
            OrderItemRepository orderItemRepository,
            DeliveryRatingRepository deliveryRatingRepository,
            RestaurantRatingRepository restaurantRatingRepository,
            CustomerOrderRepository customerOrderRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.dishRepository = dishRepository;
        this.dishCategoryRepository = dishCategoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
        this.restaurantRatingRepository = restaurantRatingRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    public boolean deleteRestaurantCascade(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            return false;
        }

        // 级联删除顺序很重要：先删最底层引用表，再删上层表，最后删 restaurant
        orderItemRepository.deleteByOrder_Restaurant_Id(restaurantId);
        deliveryRatingRepository.deleteByOrder_Restaurant_Id(restaurantId);
        restaurantRatingRepository.deleteByRestaurant_Id(restaurantId);
        customerOrderRepository.deleteByRestaurant_Id(restaurantId);
        dishRepository.deleteByRestaurant_Id(restaurantId);
        dishCategoryRepository.deleteByRestaurant_Id(restaurantId);
        restaurantUserRepository.deleteByRestaurant_Id(restaurantId);
        restaurantRepository.deleteById(restaurantId);

        return true;
    }
}

