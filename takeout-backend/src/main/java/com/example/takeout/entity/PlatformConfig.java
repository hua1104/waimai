package com.example.takeout.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "platform_config")
public class PlatformConfig {

    @Id
    private Long id = 1L;

    @Column(name = "default_commission_rate", precision = 5, scale = 2)
    private BigDecimal defaultCommissionRate;

    public PlatformConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getDefaultCommissionRate() {
        return defaultCommissionRate;
    }

    public void setDefaultCommissionRate(BigDecimal defaultCommissionRate) {
        this.defaultCommissionRate = defaultCommissionRate;
    }
}

