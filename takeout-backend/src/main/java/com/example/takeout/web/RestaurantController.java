package com.example.takeout.web;

import com.example.takeout.entity.Restaurant;
import com.example.takeout.repository.RestaurantRepository;
import com.example.takeout.service.RestaurantAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantAdminService restaurantAdminService;

    public RestaurantController(RestaurantRepository restaurantRepository, RestaurantAdminService restaurantAdminService) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantAdminService = restaurantAdminService;
    }

    @GetMapping
    public List<Restaurant> list() {
        return restaurantRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Restaurant> create(@RequestBody RestaurantPayload payload) {
        Restaurant restaurant = new Restaurant();
        applyPayload(restaurant, payload);
        Restaurant saved = restaurantRepository.save(restaurant);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> update(@PathVariable("id") Long id, @RequestBody RestaurantPayload payload) {
        Optional<Restaurant> existingOpt = restaurantRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Restaurant restaurant = existingOpt.get();
        applyPayload(restaurant, payload);
        Restaurant saved = restaurantRepository.save(restaurant);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        boolean deleted = restaurantAdminService.deleteRestaurantCascade(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private void applyPayload(Restaurant restaurant, RestaurantPayload payload) {
        restaurant.setName(payload.getName());
        restaurant.setAddress(payload.getAddress());
        restaurant.setPhone(payload.getPhone());
        restaurant.setStatus(payload.getStatus());
        restaurant.setCommissionRate(payload.getCommissionRate());
    }

    public static class RestaurantPayload {
        private String name;
        private String address;
        private String phone;
        private String status;
        private BigDecimal commissionRate;

        public RestaurantPayload() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getCommissionRate() {
            return commissionRate;
        }

        public void setCommissionRate(BigDecimal commissionRate) {
            this.commissionRate = commissionRate;
        }
    }
}
