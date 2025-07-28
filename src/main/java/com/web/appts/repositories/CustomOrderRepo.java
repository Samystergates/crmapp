package com.web.appts.repositories;

import com.web.appts.entities.CustomOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomOrderRepo extends JpaRepository<CustomOrder, Long> {
}
