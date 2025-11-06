package com.web.appts.repositories;

import com.web.appts.entities.PriceCodes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCodesRepo extends JpaRepository<PriceCodes, Long> {
}
