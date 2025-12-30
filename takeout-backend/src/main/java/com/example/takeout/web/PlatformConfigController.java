package com.example.takeout.web;

import com.example.takeout.entity.PlatformConfig;
import com.example.takeout.repository.PlatformConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class PlatformConfigController {

    private final PlatformConfigRepository platformConfigRepository;

    public PlatformConfigController(PlatformConfigRepository platformConfigRepository) {
        this.platformConfigRepository = platformConfigRepository;
    }

    @GetMapping("/commission")
    public ResponseEntity<CommissionResponse> commission() {
        PlatformConfig cfg = ensureConfig();
        BigDecimal rate = cfg.getDefaultCommissionRate() == null ? BigDecimal.ZERO : cfg.getDefaultCommissionRate();
        return ResponseEntity.ok(new CommissionResponse(rate));
    }

    @PutMapping("/commission")
    public ResponseEntity<?> updateCommission(@RequestBody CommissionPayload payload) {
        if (payload == null || payload.getDefaultCommissionRate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "defaultCommissionRate不能为空"));
        }
        BigDecimal rate = payload.getDefaultCommissionRate();
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "抽成比例必须在 0~100"));
        }

        PlatformConfig cfg = ensureConfig();
        cfg.setDefaultCommissionRate(rate);
        platformConfigRepository.save(cfg);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private PlatformConfig ensureConfig() {
        return platformConfigRepository.findById(1L).orElseGet(() -> {
            PlatformConfig cfg = new PlatformConfig();
            cfg.setId(1L);
            cfg.setDefaultCommissionRate(BigDecimal.ZERO);
            return platformConfigRepository.save(cfg);
        });
    }

    public static class CommissionPayload {
        private BigDecimal defaultCommissionRate;

        public CommissionPayload() {
        }

        public BigDecimal getDefaultCommissionRate() {
            return defaultCommissionRate;
        }

        public void setDefaultCommissionRate(BigDecimal defaultCommissionRate) {
            this.defaultCommissionRate = defaultCommissionRate;
        }
    }

    public static class CommissionResponse {
        private BigDecimal defaultCommissionRate;

        public CommissionResponse(BigDecimal defaultCommissionRate) {
            this.defaultCommissionRate = defaultCommissionRate;
        }

        public BigDecimal getDefaultCommissionRate() {
            return defaultCommissionRate;
        }
    }
}

