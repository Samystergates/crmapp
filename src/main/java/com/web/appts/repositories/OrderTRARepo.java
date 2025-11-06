
package com.web.appts.repositories;

import com.web.appts.entities.OrderTRA;
import com.web.appts.utils.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderTRARepo extends JpaRepository<OrderTRA, Long> {
    Optional<OrderTRA> findByOrderIds_IdAndOrderIds_OrderType(Long orderId, OrderType type);

}
