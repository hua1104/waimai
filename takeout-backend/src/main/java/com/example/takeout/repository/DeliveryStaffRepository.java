package com.example.takeout.repository;

import com.example.takeout.entity.DeliveryStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeliveryStaffRepository extends JpaRepository<DeliveryStaff, Long> {

    @Query("select d from DeliveryStaff d where d.status = 'ACTIVE' order by coalesce(d.currentLoad, 0) asc, d.id asc")
    List<DeliveryStaff> findAssignable();
}
