package com.web.appts.repositories;

import com.web.appts.entities.OrderDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDepartmentRepo extends JpaRepository<OrderDepartment, Long> {
    List<OrderDepartment> findByOrderId(Integer orderId);
}
