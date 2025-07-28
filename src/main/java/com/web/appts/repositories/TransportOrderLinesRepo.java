package com.web.appts.repositories;

import com.web.appts.entities.TransportOrderLines;
import com.web.appts.utils.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransportOrderLinesRepo extends JpaRepository<TransportOrderLines, Long> {

    Optional<TransportOrderLines> findByOrderIdAndOrderType(long orderId, OrderType orderType);
}
