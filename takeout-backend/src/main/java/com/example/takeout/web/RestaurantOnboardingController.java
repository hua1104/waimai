package com.example.takeout.web;

import com.example.takeout.entity.RestaurantApplication;
import com.example.takeout.repository.RestaurantApplicationRepository;
import com.example.takeout.repository.RestaurantUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public/restaurant-applications")
public class RestaurantOnboardingController {

    private final RestaurantApplicationRepository applicationRepository;
    private final RestaurantUserRepository restaurantUserRepository;

    public RestaurantOnboardingController(RestaurantApplicationRepository applicationRepository,
                                         RestaurantUserRepository restaurantUserRepository) {
        this.applicationRepository = applicationRepository;
        this.restaurantUserRepository = restaurantUserRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> apply(@RequestBody ApplyPayload payload) {
        if (payload == null
                || isBlank(payload.getRestaurantName())
                || isBlank(payload.getUsername())
                || isBlank(payload.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "餐馆名称、用户名、密码不能为空"));
        }

        String username = payload.getUsername().trim();
        if (restaurantUserRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在"));
        }
        if (applicationRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该用户名已提交过入驻申请"));
        }

        RestaurantApplication a = new RestaurantApplication();
        a.setRestaurantName(payload.getRestaurantName().trim());
        a.setAddress(safe(payload.getAddress(), 200));
        a.setPhone(safe(payload.getPhone(), 20));
        a.setUsername(username);
        a.setPassword(payload.getPassword());
        a.setLicenseNo(safe(payload.getLicenseNo(), 50));
        a.setContactName(safe(payload.getContactName(), 50));
        a.setContactPhone(safe(payload.getContactPhone(), 20));
        a.setStatus("PENDING");

        RestaurantApplication saved = applicationRepository.save(a);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", saved.getStatus()));
    }

    @GetMapping("/latest")
    public ResponseEntity<?> latest(@RequestParam("username") String username) {
        if (isBlank(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "username不能为空"));
        }
        Optional<RestaurantApplication> opt = applicationRepository.findTopByUsernameOrderByCreatedAtDesc(username.trim());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RestaurantApplication a = opt.get();
        return ResponseEntity.ok(new ApplicationStatusRow(
                a.getId(),
                a.getRestaurantName(),
                a.getStatus(),
                a.getReviewRemark(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                a.getReviewedAt() != null ? a.getReviewedAt().toString() : null,
                a.getRestaurantId(),
                a.getRestaurantUserId()
        ));
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String safe(String v, int maxLen) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    public static class ApplyPayload {
        private String restaurantName;
        private String address;
        private String phone;
        private String username;
        private String password;
        private String licenseNo;
        private String contactName;
        private String contactPhone;

        public ApplyPayload() {
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public void setRestaurantName(String restaurantName) {
            this.restaurantName = restaurantName;
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

        public String getLicenseNo() {
            return licenseNo;
        }

        public void setLicenseNo(String licenseNo) {
            this.licenseNo = licenseNo;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }
    }

    public static class ApplicationStatusRow {
        private Long id;
        private String restaurantName;
        private String status;
        private String reviewRemark;
        private String createdAt;
        private String reviewedAt;
        private Long restaurantId;
        private Long restaurantUserId;

        public ApplicationStatusRow(Long id,
                                    String restaurantName,
                                    String status,
                                    String reviewRemark,
                                    String createdAt,
                                    String reviewedAt,
                                    Long restaurantId,
                                    Long restaurantUserId) {
            this.id = id;
            this.restaurantName = restaurantName;
            this.status = status;
            this.reviewRemark = reviewRemark;
            this.createdAt = createdAt;
            this.reviewedAt = reviewedAt;
            this.restaurantId = restaurantId;
            this.restaurantUserId = restaurantUserId;
        }

        public Long getId() {
            return id;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public String getStatus() {
            return status;
        }

        public String getReviewRemark() {
            return reviewRemark;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getReviewedAt() {
            return reviewedAt;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public Long getRestaurantUserId() {
            return restaurantUserId;
        }
    }
}

