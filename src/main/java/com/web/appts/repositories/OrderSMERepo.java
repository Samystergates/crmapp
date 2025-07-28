
package com.web.appts.repositories;

import com.web.appts.entities.OrderSME;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSMERepo extends JpaRepository<OrderSME, Long> {
	OrderSME findByOrderNumberAndProdNumber(String paramString1, String paramString2);
	OrderSME findByOrderNumberAndRegel(String paramString1, String paramString2);
	OrderSME findTopByOrderByIdDesc();
}
