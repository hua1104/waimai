package com.example.takeout.web;

import com.example.takeout.entity.Customer;
import com.example.takeout.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/profile")
public class CustomerProfileController {

    private final CustomerRepository customerRepository;

    public CustomerProfileController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestParam("customerId") Long customerId) {
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "customerId required"));
        }
        Optional<Customer> opt = customerRepository.findById(customerId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "customer not found"));
        }
        Customer c = opt.get();
        return ResponseEntity.ok(new ProfileRow(
                c.getId(),
                c.getUsername(),
                c.getRealName(),
                c.getPhone(),
                c.getStatus(),
                c.getCreatedAt() == null ? null : c.getCreatedAt().toString()
        ));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfilePayload payload) {
        String err = validateProfile(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }
        Optional<Customer> opt = customerRepository.findById(payload.getCustomerId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "customer not found"));
        }
        Customer c = opt.get();
        c.setRealName(payload.getRealName().trim());
        c.setPhone(payload.getPhone().trim());
        customerRepository.save(c);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @PutMapping("/password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordPayload payload) {
        String err = validatePassword(payload);
        if (err != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", err));
        }
        Optional<Customer> opt = customerRepository.findById(payload.getCustomerId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "customer not found"));
        }
        Customer c = opt.get();
        if (c.getPassword() == null || !c.getPassword().equals(payload.getOldPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "old password incorrect"));
        }
        if (payload.getOldPassword().equals(payload.getNewPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "new password must be different"));
        }
        c.setPassword(payload.getNewPassword());
        customerRepository.save(c);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private static String validateProfile(UpdateProfilePayload payload) {
        if (payload == null || payload.getCustomerId() == null) return "invalid payload";
        if (isBlank(payload.getRealName())) return "realName required";
        if (isBlank(payload.getPhone())) return "phone required";
        if (payload.getRealName().trim().length() > 50) return "realName too long";
        if (payload.getPhone().trim().length() > 20) return "phone too long";
        return null;
    }

    private static String validatePassword(ChangePasswordPayload payload) {
        if (payload == null || payload.getCustomerId() == null) return "invalid payload";
        if (isBlank(payload.getOldPassword())) return "oldPassword required";
        if (isBlank(payload.getNewPassword())) return "newPassword required";
        if (payload.getNewPassword().length() < 6) return "newPassword must be at least 6 chars";
        if (payload.getNewPassword().length() > 100) return "newPassword too long";
        return null;
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static class UpdateProfilePayload {
        private Long customerId;
        private String realName;
        private String phone;

        public UpdateProfilePayload() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class ChangePasswordPayload {
        private Long customerId;
        private String oldPassword;
        private String newPassword;

        public ChangePasswordPayload() {
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class ProfileRow {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private String status;
        private String createdAt;

        public ProfileRow(Long id,
                          String username,
                          String realName,
                          String phone,
                          String status,
                          String createdAt) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.phone = phone;
            this.status = status;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
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

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}

