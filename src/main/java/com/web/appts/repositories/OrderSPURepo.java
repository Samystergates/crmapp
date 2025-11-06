
package com.web.appts.repositories;

import com.web.appts.entities.OrderSPU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface OrderSPURepo extends JpaRepository<OrderSPU, Long> {
	OrderSPU findByOrderNumberAndProdNumber(String paramString1, String paramString2);
	OrderSPU findByOrderNumberAndRegel(String paramString1, String paramString2);

	@Modifying
	@Transactional
	@Query("DELETE FROM OrderSPU spu WHERE spu.orderNumber = :orderNumber AND spu.regel = :regel")
	int deleteSPUByOrderAndRegel(@Param("orderNumber") String orderNumber, @Param("regel") String regel);
}
