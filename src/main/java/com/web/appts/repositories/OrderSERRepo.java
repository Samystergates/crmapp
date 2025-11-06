package com.web.appts.repositories;

import com.web.appts.entities.OrderSER;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSERRepo extends JpaRepository<OrderSER, Long> {
    OrderSER findByOrderNumber(String orderNumber);
}
