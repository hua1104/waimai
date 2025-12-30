package com.example.takeout.web;

import com.example.takeout.entity.Restaurant;
import com.example.takeout.entity.RestaurantApplication;
import com.example.takeout.entity.RestaurantUser;
import com.example.takeout.repository.RestaurantApplicationRepository;
import com.example.takeout.repository.RestaurantRepository;
import com.example.takeout.repository.RestaurantUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/restaurant-applications")
public class RestaurantApplicationAdminController {

    private final RestaurantApplicationRepository applicationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserRepository restaurantUserRepository;

    public RestaurantApplicationAdminController(RestaurantApplicationRepository applicationRepository,
                                               RestaurantRepository restaurantRepository,
                                               RestaurantUserRepository restaurantUserRepository) {
        this.applicationRepository = applicationRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantUserRepository = restaurantUserRepository;
    }

    @GetMapping
    public List<ApplicationRow> list(@RequestParam(value = "status", required = false) String status) {
        return applicationRepository.findAdminList(blankToNull(status)).stream()
                .map(a -> new ApplicationRow(
                        a.getId(),
                        a.getRestaurantName(),
                        a.getAddress(),
                        a.getPhone(),
                        a.getUsername(),
                        a.getLicenseNo(),
                        a.getContactName(),
                        a.getContactPhone(),
                        a.getStatus(),
                        a.getReviewRemark(),
                        a.getRestaurantId(),
                        a.getRestaurantUserId(),
                        a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                        a.getReviewedAt() != null ? a.getReviewedAt().toString() : null
                ))
                .toList();
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable("id") Long id, @RequestBody ReviewPayload payload) {
        Optional<RestaurantApplication> opt = applicationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantApplication a = opt.get();

        if (!"PENDING".equalsIgnoreCase(a.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该申请不是待审核状态"));
        }
        if (restaurantUserRepository.existsByUsername(a.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该用户名已被占用"));
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(a.getRestaurantName());
        restaurant.setAddress(a.getAddress());
        restaurant.setPhone(a.getPhone());
        restaurant.setStatus("ACTIVE");
        Restaurant restaurantSaved = restaurantRepository.save(restaurant);

        RestaurantUser ru = new RestaurantUser();
        ru.setRestaurant(restaurantSaved);
        ru.setUsername(a.getUsername());
        ru.setPassword(a.getPassword());
        ru.setStatus("ACTIVE");
        RestaurantUser ruSaved = restaurantUserRepository.save(ru);

        a.setStatus("APPROVED");
        a.setReviewRemark(safe(payload == null ? null : payload.getRemark(), 200));
        a.setReviewedAt(LocalDateTime.now());
        a.setRestaurantId(restaurantSaved.getId());
        a.setRestaurantUserId(ruSaved.getId());
        applicationRepository.save(a);

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "restaurantId", restaurantSaved.getId(),
                "restaurantUserId", ruSaved.getId()
        ));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable("id") Long id, @RequestBody ReviewPayload payload) {
        Optional<RestaurantApplication> opt = applicationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantApplication a = opt.get();

        if (!"PENDING".equalsIgnoreCase(a.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该申请不是待审核状态"));
        }

        a.setStatus("REJECTED");
        a.setReviewRemark(safe(payload == null ? null : payload.getRemark(), 200));
        a.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(a);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safe(String v, int maxLen) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    public static class ReviewPayload {
        private String remark;

        public ReviewPayload() {
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class ApplicationRow {
        private Long id;
        private String restaurantName;
        private String address;
        private String phone;
        private String username;
        private String licenseNo;
        private String contactName;
        private String contactPhone;
        private String status;
        private String reviewRemark;
        private Long restaurantId;
        private Long restaurantUserId;
        private String createdAt;
        private String reviewedAt;

        public ApplicationRow(Long id,
                              String restaurantName,
                              String address,
                              String phone,
                              String username,
                              String licenseNo,
                              String contactName,
                              String contactPhone,
                              String status,
                              String reviewRemark,
                              Long restaurantId,
                              Long restaurantUserId,
                              String createdAt,
                              String reviewedAt) {
            this.id = id;
            this.restaurantName = restaurantName;
            this.address = address;
            this.phone = phone;
            this.username = username;
            this.licenseNo = licenseNo;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.status = status;
            this.reviewRemark = reviewRemark;
            this.restaurantId = restaurantId;
            this.restaurantUserId = restaurantUserId;
            this.createdAt = createdAt;
            this.reviewedAt = reviewedAt;
        }

        public Long getId() {
            return id;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }

        public String getUsername() {
            return username;
        }

        public String getLicenseNo() {
            return licenseNo;
        }

        public String getContactName() {
            return contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public String getStatus() {
            return status;
        }

        public String getReviewRemark() {
            return reviewRemark;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public Long getRestaurantUserId() {
            return restaurantUserId;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getReviewedAt() {
            return reviewedAt;
        }
    }
}

