package com.example.takeout.web;

import com.example.takeout.entity.Dish;
import com.example.takeout.entity.DishCategory;
import com.example.takeout.entity.Restaurant;
import com.example.takeout.repository.DishCategoryRepository;
import com.example.takeout.repository.DishRepository;
import com.example.takeout.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/merchant")
public class MerchantCatalogController {

    private final RestaurantRepository restaurantRepository;
    private final DishCategoryRepository dishCategoryRepository;
    private final DishRepository dishRepository;

    public MerchantCatalogController(RestaurantRepository restaurantRepository,
                                     DishCategoryRepository dishCategoryRepository,
                                     DishRepository dishRepository) {
        this.restaurantRepository = restaurantRepository;
        this.dishCategoryRepository = dishCategoryRepository;
        this.dishRepository = dishRepository;
    }

    @GetMapping("/{restaurantId}/categories")
    public ResponseEntity<List<CategoryRow>> listCategories(@PathVariable("restaurantId") Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build();
        }
        List<CategoryRow> rows = dishCategoryRepository.findByRestaurant_Id(restaurantId).stream()
                .map(MerchantCatalogController::toRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{restaurantId}/categories")
    public ResponseEntity<CategoryRow> createCategory(@PathVariable("restaurantId") Long restaurantId,
                                                      @RequestBody CategoryPayload payload) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DishCategory category = new DishCategory();
        category.setRestaurant(restaurantOpt.get());
        category.setName(payload.getName());
        category.setSortOrder(payload.getSortOrder());
        DishCategory saved = dishCategoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRow(saved));
    }

    @DeleteMapping("/{restaurantId}/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("restaurantId") Long restaurantId,
                                               @PathVariable("id") Long categoryId) {
        Optional<DishCategory> categoryOpt = dishCategoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DishCategory category = categoryOpt.get();
        if (category.getRestaurant() == null || category.getRestaurant().getId() == null
                || !category.getRestaurant().getId().equals(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        long used = dishRepository.countByCategory_Id(categoryId);
        if (used > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        dishCategoryRepository.deleteById(categoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{restaurantId}/dishes")
    public ResponseEntity<List<DishRow>> listDishes(@PathVariable("restaurantId") Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            return ResponseEntity.notFound().build();
        }
        List<DishRow> rows = dishRepository.findByRestaurant_Id(restaurantId).stream()
                .map(MerchantCatalogController::toRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{restaurantId}/dishes")
    public ResponseEntity<DishRow> createDish(@PathVariable("restaurantId") Long restaurantId, @RequestBody DishPayload payload) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dish dish = new Dish();
        dish.setRestaurant(restaurantOpt.get());
        applyDishPayload(dish, restaurantId, payload);
        Dish saved = dishRepository.save(dish);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRow(saved));
    }

    @PutMapping("/{restaurantId}/dishes/{id}")
    public ResponseEntity<DishRow> updateDish(@PathVariable("restaurantId") Long restaurantId,
                                              @PathVariable("id") Long dishId,
                                              @RequestBody DishPayload payload) {
        Optional<Dish> dishOpt = dishRepository.findById(dishId);
        if (dishOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dish dish = dishOpt.get();
        if (dish.getRestaurant() == null || dish.getRestaurant().getId() == null
                || !dish.getRestaurant().getId().equals(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        applyDishPayload(dish, restaurantId, payload);
        Dish saved = dishRepository.save(dish);
        return ResponseEntity.ok(toRow(saved));
    }

    @DeleteMapping("/{restaurantId}/dishes/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable("restaurantId") Long restaurantId,
                                           @PathVariable("id") Long dishId) {
        Optional<Dish> dishOpt = dishRepository.findById(dishId);
        if (dishOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dish dish = dishOpt.get();
        if (dish.getRestaurant() == null || dish.getRestaurant().getId() == null
                || !dish.getRestaurant().getId().equals(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        dishRepository.deleteById(dishId);
        return ResponseEntity.noContent().build();
    }

    private void applyDishPayload(Dish dish, Long restaurantId, DishPayload payload) {
        dish.setName(payload.getName());
        dish.setPrice(payload.getPrice() == null ? BigDecimal.ZERO : payload.getPrice());
        dish.setStatus(payload.getStatus());
        dish.setStock(payload.getStock());
        dish.setDiscountType(payload.getDiscountType());
        dish.setDiscountValue(payload.getDiscountValue());

        if (payload.getCategoryId() == null) {
            dish.setCategory(null);
        } else {
            Optional<DishCategory> catOpt = dishCategoryRepository.findById(payload.getCategoryId());
            if (catOpt.isPresent()
                    && catOpt.get().getRestaurant() != null
                    && catOpt.get().getRestaurant().getId() != null
                    && catOpt.get().getRestaurant().getId().equals(restaurantId)) {
                dish.setCategory(catOpt.get());
            } else {
                dish.setCategory(null);
            }
        }
    }

    private static CategoryRow toRow(DishCategory category) {
        return new CategoryRow(category.getId(), category.getName(), category.getSortOrder());
    }

    private static DishRow toRow(Dish dish) {
        Long categoryId = null;
        String categoryName = null;
        if (dish.getCategory() != null) {
            categoryId = dish.getCategory().getId();
            categoryName = dish.getCategory().getName();
        }
        return new DishRow(
                dish.getId(),
                categoryId,
                categoryName,
                dish.getName(),
                dish.getPrice(),
                dish.getStock(),
                dish.getStatus(),
                dish.getDiscountType(),
                dish.getDiscountValue()
        );
    }

    public static class CategoryRow {
        private Long id;
        private String name;
        private Integer sortOrder;

        public CategoryRow(Long id, String name, Integer sortOrder) {
            this.id = id;
            this.name = name;
            this.sortOrder = sortOrder;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }
    }

    public static class DishRow {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private String status;
        private String discountType;
        private BigDecimal discountValue;

        public DishRow(Long id,
                       Long categoryId,
                       String categoryName,
                       String name,
                       BigDecimal price,
                       Integer stock,
                       String status,
                       String discountType,
                       BigDecimal discountValue) {
            this.id = id;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.name = name;
            this.price = price;
            this.stock = stock;
            this.status = status;
            this.discountType = discountType;
            this.discountValue = discountValue;
        }

        public Long getId() {
            return id;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public Integer getStock() {
            return stock;
        }

        public String getStatus() {
            return status;
        }

        public String getDiscountType() {
            return discountType;
        }

        public BigDecimal getDiscountValue() {
            return discountValue;
        }
    }

    public static class CategoryPayload {
        private String name;
        private Integer sortOrder;

        public CategoryPayload() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    public static class DishPayload {
        private Long categoryId;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private String status; // AVAILABLE, UNAVAILABLE
        private String discountType;
        private BigDecimal discountValue;

        public DishPayload() {
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDiscountType() {
            return discountType;
        }

        public void setDiscountType(String discountType) {
            this.discountType = discountType;
        }

        public BigDecimal getDiscountValue() {
            return discountValue;
        }

        public void setDiscountValue(BigDecimal discountValue) {
            this.discountValue = discountValue;
        }
    }
}
