package com.example.takeout.repository;

import com.example.takeout.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByCustomer_IdOrderByIsDefaultDescIdDesc(Long customerId);

    Optional<CustomerAddress> findFirstByCustomer_IdAndIsDefaultTrue(Long customerId);

    long deleteByCustomer_Id(Long customerId);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault = false where a.customer.id = :customerId")
    int clearDefault(@Param("customerId") Long customerId);
}

