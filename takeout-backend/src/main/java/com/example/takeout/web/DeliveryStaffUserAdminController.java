package com.example.takeout.web;

import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.entity.DeliveryStaffUser;
import com.example.takeout.repository.DeliveryStaffRepository;
import com.example.takeout.repository.DeliveryStaffUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery-staff-users")
public class DeliveryStaffUserAdminController {

    private final DeliveryStaffUserRepository deliveryStaffUserRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;

    public DeliveryStaffUserAdminController(DeliveryStaffUserRepository deliveryStaffUserRepository,
                                            DeliveryStaffRepository deliveryStaffRepository) {
        this.deliveryStaffUserRepository = deliveryStaffUserRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
    }

    @GetMapping
    public List<DeliveryStaffUserRow> list() {
        return deliveryStaffUserRepository.findAll().stream()
                .map(u -> new DeliveryStaffUserRow(
                        u.getId(),
                        u.getDeliveryStaff() != null ? u.getDeliveryStaff().getId() : null,
                        u.getDeliveryStaff() != null ? u.getDeliveryStaff().getName() : null,
                        u.getUsername(),
                        u.getStatus()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreatePayload payload) {
        if (payload == null
                || payload.getDeliveryStaffId() == null
                || payload.getUsername() == null
                || payload.getUsername().isBlank()
                || payload.getPassword() == null
                || payload.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "参数不完整"));
        }
        if (deliveryStaffUserRepository.existsByUsername(payload.getUsername().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在"));
        }
        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(payload.getDeliveryStaffId());
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "骑手不存在"));
        }

        DeliveryStaffUser u = new DeliveryStaffUser();
        u.setDeliveryStaff(staffOpt.get());
        u.setUsername(payload.getUsername().trim());
        u.setPassword(payload.getPassword());
        u.setStatus(payload.getStatus() == null || payload.getStatus().isBlank() ? "ACTIVE" : payload.getStatus());

        DeliveryStaffUser saved = deliveryStaffUserRepository.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "status", "OK"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody UpdatePayload payload) {
        Optional<DeliveryStaffUser> opt = deliveryStaffUserRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryStaffUser u = opt.get();

        if (payload != null) {
            if (payload.getStatus() != null) {
                u.setStatus(payload.getStatus());
            }
            if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
                u.setPassword(payload.getPassword());
            }
        }
        deliveryStaffUserRepository.save(u);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        if (!deliveryStaffUserRepository.existsById(id)) return ResponseEntity.notFound().build();
        deliveryStaffUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static class DeliveryStaffUserRow {
        private final Long id;
        private final Long deliveryStaffId;
        private final String deliveryStaffName;
        private final String username;
        private final String status;

        public DeliveryStaffUserRow(Long id,
                                    Long deliveryStaffId,
                                    String deliveryStaffName,
                                    String username,
                                    String status) {
            this.id = id;
            this.deliveryStaffId = deliveryStaffId;
            this.deliveryStaffName = deliveryStaffName;
            this.username = username;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public String getDeliveryStaffName() {
            return deliveryStaffName;
        }

        public String getUsername() {
            return username;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class CreatePayload {
        private Long deliveryStaffId;
        private String username;
        private String password;
        private String status;

        public CreatePayload() {
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public void setDeliveryStaffId(Long deliveryStaffId) {
            this.deliveryStaffId = deliveryStaffId;
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

