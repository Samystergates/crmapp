
package com.web.appts.repositories;

import com.web.appts.entities.ArchivedOrders;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchivedOrderRepo extends JpaRepository<ArchivedOrders, Long> {
  List<ArchivedOrders> findByUser(String paramString);

  List<ArchivedOrders> findByOrderNumber(String paramString);

  Optional<ArchivedOrders> findByOrderNumberAndRegel(String orderNumber, String regel);

  List<ArchivedOrders> findByRegel(String regel);

  @Query("SELECT a FROM ArchivedOrders a WHERE a.regel = :regel")
  List<ArchivedOrders> findArchivedOrdersByRegel(@Param("regel") String regel);
  


}
