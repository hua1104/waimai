package com.example.takeout.web;

import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.repository.DeliveryStaffRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery-staff")
public class DeliveryStaffAdminController {

    private final DeliveryStaffRepository deliveryStaffRepository;

    public DeliveryStaffAdminController(DeliveryStaffRepository deliveryStaffRepository) {
        this.deliveryStaffRepository = deliveryStaffRepository;
    }

    @GetMapping
    public List<DeliveryStaff> list() {
        return deliveryStaffRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Payload payload) {
        if (payload == null || payload.getName() == null || payload.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "姓名不能为空"));
        }
        DeliveryStaff staff = new DeliveryStaff();
        staff.setName(payload.getName().trim());
        staff.setPhone(payload.getPhone());
        staff.setStatus(payload.getStatus() == null || payload.getStatus().isBlank() ? "ACTIVE" : payload.getStatus());
        staff.setCurrentLoad(payload.getCurrentLoad() == null ? 0 : payload.getCurrentLoad());
        DeliveryStaff saved = deliveryStaffRepository.save(staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Payload payload) {
        Optional<DeliveryStaff> opt = deliveryStaffRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryStaff staff = opt.get();

        if (payload != null) {
            if (payload.getName() != null) staff.setName(payload.getName().trim());
            if (payload.getPhone() != null) staff.setPhone(payload.getPhone());
            if (payload.getStatus() != null) staff.setStatus(payload.getStatus());
            if (payload.getCurrentLoad() != null) staff.setCurrentLoad(payload.getCurrentLoad());
        }

        DeliveryStaff saved = deliveryStaffRepository.save(staff);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        if (!deliveryStaffRepository.existsById(id)) return ResponseEntity.notFound().build();
        deliveryStaffRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static class Payload {
        private String name;
        private String phone;
        private String status;
        private Integer currentLoad;

        public Payload() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public Integer getCurrentLoad() {
            return currentLoad;
        }

        public void setCurrentLoad(Integer currentLoad) {
            this.currentLoad = currentLoad;
        }
    }
}

