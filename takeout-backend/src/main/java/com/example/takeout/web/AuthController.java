package com.example.takeout.web;

import com.example.takeout.entity.Admin;
import com.example.takeout.entity.Customer;
import com.example.takeout.entity.DeliveryStaffUser;
import com.example.takeout.entity.RestaurantUser;
import com.example.takeout.repository.AdminRepository;
import com.example.takeout.repository.CustomerRepository;
import com.example.takeout.repository.DeliveryStaffUserRepository;
import com.example.takeout.repository.RestaurantUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminRepository adminRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryStaffUserRepository deliveryStaffUserRepository;

    public AuthController(AdminRepository adminRepository,
                          RestaurantUserRepository restaurantUserRepository,
                          CustomerRepository customerRepository,
                          DeliveryStaffUserRepository deliveryStaffUserRepository) {
        this.adminRepository = adminRepository;
        this.restaurantUserRepository = restaurantUserRepository;
        this.customerRepository = customerRepository;
        this.deliveryStaffUserRepository = deliveryStaffUserRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = request.getRole();
        String username = request.getUsername();
        String password = request.getPassword();

        if (role == null || username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "用户名、密码和角色不能为空"));
        }

        switch (role) {
            case "ADMIN": {
                Optional<Admin> adminOpt = adminRepository.findByUsernameAndPassword(username, password);
                if (adminOpt.isEmpty()) {
                    return unauthorized();
                }
                Admin admin = adminOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                    return unauthorized();
                }
                return ResponseEntity.ok(new LoginResponse(
                        admin.getId(),
                        "ADMIN",
                        null,
                        null,
                        UUID.randomUUID().toString()
                ));
            }
            case "RESTAURANT": {
                Optional<RestaurantUser> ruOpt = restaurantUserRepository.findByUsernameAndPassword(username, password);
                if (ruOpt.isEmpty()) {
                    return unauthorized();
                }
                RestaurantUser ru = ruOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(ru.getStatus())) {
                    return unauthorized();
                }
                if (ru.getRestaurant() == null
                        || ru.getRestaurant().getId() == null
                        || (ru.getRestaurant().getStatus() != null && !"ACTIVE".equalsIgnoreCase(ru.getRestaurant().getStatus()))) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "饭店未通过认证或已停用"));
                }
                return ResponseEntity.ok(new LoginResponse(
                        ru.getId(),
                        "RESTAURANT",
                        ru.getRestaurant() != null ? ru.getRestaurant().getId() : null,
                        null,
                        UUID.randomUUID().toString()
                ));
            }
            case "CUSTOMER": {
                Optional<Customer> cuOpt = customerRepository.findByUsernameAndPassword(username, password);
                if (cuOpt.isEmpty()) {
                    return unauthorized();
                }
                Customer cu = cuOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(cu.getStatus())) {
                    return unauthorized();
                }
                return ResponseEntity.ok(new LoginResponse(
                        cu.getId(),
                        "CUSTOMER",
                        null,
                        null,
                        UUID.randomUUID().toString()
                ));
            }
            case "DELIVERY": {
                Optional<DeliveryStaffUser> duOpt = deliveryStaffUserRepository.findByUsernameAndPassword(username, password);
                if (duOpt.isEmpty()) {
                    return unauthorized();
                }
                DeliveryStaffUser du = duOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(du.getStatus())) {
                    return unauthorized();
                }
                return ResponseEntity.ok(new LoginResponse(
                        du.getId(),
                        "DELIVERY",
                        null,
                        du.getDeliveryStaff() != null ? du.getDeliveryStaff().getId() : null,
                        UUID.randomUUID().toString()
                ));
            }
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "不支持的角色类型"));
        }
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "用户名或密码错误，或账号已禁用"));
    }

    public static class LoginRequest {
        private String username;
        private String password;
        private String role;

        public LoginRequest() {
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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class LoginResponse {
        private Long userId;
        private String role;
        private Long restaurantId;
        private Long deliveryStaffId;
        private String token;

        public LoginResponse(Long userId, String role, Long restaurantId, Long deliveryStaffId, String token) {
            this.userId = userId;
            this.role = role;
            this.restaurantId = restaurantId;
            this.deliveryStaffId = deliveryStaffId;
            this.token = token;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public String getToken() {
            return token;
        }
    }
}
