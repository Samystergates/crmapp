
package com.web.appts.repositories;

import com.web.appts.entities.OrderSME;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface OrderSMERepo extends JpaRepository<OrderSME, Long> {
    OrderSME findByOrderNumberAndProdNumber(String paramString1, String paramString2);

    OrderSME findByOrderNumberAndRegel(String paramString1, String paramString2);

    OrderSME findTopByOrderByIdDesc();

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderSME sme WHERE sme.orderNumber = :orderNumber AND sme.regel = :regel")
    int deleteSMEByOrderAndRegel(@Param("orderNumber") String orderNumber, @Param("regel") String regel);
}
