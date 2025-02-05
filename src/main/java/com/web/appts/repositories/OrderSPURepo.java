
package com.web.appts.repositories;

import com.web.appts.entities.OrderSPU;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSPURepo extends JpaRepository<OrderSPU, Long> {
	OrderSPU findByOrderNumberAndProdNumber(String paramString1, String paramString2);
	OrderSPU findByOrderNumberAndRegel(String paramString1, String paramString2);
}
