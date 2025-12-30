package com.example.takeout.web;

import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.repository.DeliveryStaffRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rider")
public class RiderLocationController {

    private final DeliveryStaffRepository deliveryStaffRepository;

    public RiderLocationController(DeliveryStaffRepository deliveryStaffRepository) {
        this.deliveryStaffRepository = deliveryStaffRepository;
    }

    @PutMapping("/location")
    @Transactional
    public ResponseEntity<?> updateLocation(@RequestBody UpdateLocationPayload payload) {
        if (payload == null || payload.getDeliveryStaffId() == null || payload.getLat() == null || payload.getLng() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "参数不完整"));
        }
        if (!isValidLatLng(payload.getLat(), payload.getLng())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "坐标不合法"));
        }
        Optional<DeliveryStaff> opt = deliveryStaffRepository.findById(payload.getDeliveryStaffId());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryStaff s = opt.get();
        s.setCurrentLat(payload.getLat());
        s.setCurrentLng(payload.getLng());
        s.setLocationUpdatedAt(LocalDateTime.now());
        deliveryStaffRepository.save(s);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private static boolean isValidLatLng(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    public static class UpdateLocationPayload {
        private Long deliveryStaffId;
        private Double lat;
        private Double lng;

        public UpdateLocationPayload() {
        }

        public Long getDeliveryStaffId() {
            return deliveryStaffId;
        }

        public void setDeliveryStaffId(Long deliveryStaffId) {
            this.deliveryStaffId = deliveryStaffId;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }
}

