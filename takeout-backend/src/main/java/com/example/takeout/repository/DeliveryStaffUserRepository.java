package com.example.takeout.repository;

import com.example.takeout.entity.DeliveryStaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryStaffUserRepository extends JpaRepository<DeliveryStaffUser, Long> {

    Optional<DeliveryStaffUser> findByUsernameAndPassword(String username, String password);

    boolean existsByUsername(String username);
}
