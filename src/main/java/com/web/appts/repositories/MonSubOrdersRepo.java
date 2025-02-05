package com.web.appts.repositories;

import com.web.appts.entities.MonSubOrders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonSubOrdersRepo extends JpaRepository<MonSubOrders, Long> {
    Optional<MonSubOrders> findByOrderNumberAndRegelAndProduct(String orderNumber, String regel, String product);


}
