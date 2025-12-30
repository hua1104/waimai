package com.example.takeout.repository;

import com.example.takeout.entity.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {
}

