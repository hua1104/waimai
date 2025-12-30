package com.example.takeout.web;

import com.example.takeout.entity.Restaurant;
import com.example.takeout.entity.RestaurantUser;
import com.example.takeout.repository.RestaurantRepository;
import com.example.takeout.repository.RestaurantUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/restaurant-users")
public class RestaurantUserAdminController {

    private final RestaurantUserRepository restaurantUserRepository;
    private final RestaurantRepository restaurantRepository;

    public RestaurantUserAdminController(RestaurantUserRepository restaurantUserRepository,
                                         RestaurantRepository restaurantRepository) {
        this.restaurantUserRepository = restaurantUserRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @GetMapping
    public List<RestaurantUserRow> list() {
        return restaurantUserRepository.findAll().stream()
                .map(u -> new RestaurantUserRow(
                        u.getId(),
                        u.getRestaurant() != null ? u.getRestaurant().getId() : null,
                        u.getRestaurant() != null ? u.getRestaurant().getName() : null,
                        u.getUsername(),
                        u.getStatus()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreatePayload payload) {
        if (payload == null
                || payload.getRestaurantId() == null
                || payload.getUsername() == null
                || payload.getUsername().isBlank()
                || payload.getPassword() == null
                || payload.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "参数不完整"));
        }
        String username = payload.getUsername().trim();
        if (restaurantUserRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在"));
        }
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(payload.getRestaurantId());
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "饭店不存在"));
        }

        RestaurantUser u = new RestaurantUser();
        u.setRestaurant(restaurantOpt.get());
        u.setUsername(username);
        u.setPassword(payload.getPassword());
        u.setStatus(payload.getStatus() == null || payload.getStatus().isBlank() ? "ACTIVE" : payload.getStatus());

        RestaurantUser saved = restaurantUserRepository.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody UpdatePayload payload) {
        Optional<RestaurantUser> opt = restaurantUserRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantUser u = opt.get();

        if (payload != null) {
            if (payload.getStatus() != null) {
                u.setStatus(payload.getStatus());
            }
            if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
                u.setPassword(payload.getPassword());
            }
        }
        restaurantUserRepository.save(u);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        if (!restaurantUserRepository.existsById(id)) return ResponseEntity.notFound().build();
        restaurantUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static class RestaurantUserRow {
        private final Long id;
        private final Long restaurantId;
        private final String restaurantName;
        private final String username;
        private final String status;

        public RestaurantUserRow(Long id,
                                 Long restaurantId,
                                 String restaurantName,
                                 String username,
                                 String status) {
            this.id = id;
            this.restaurantId = restaurantId;
            this.restaurantName = restaurantName;
            this.username = username;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public String getUsername() {
            return username;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class CreatePayload {
        private Long restaurantId;
        private String username;
        private String password;
        private String status;

        public CreatePayload() {
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class UpdatePayload {
        private String password;
        private String status;

        public UpdatePayload() {
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}

