
package com.web.appts.repositories;

import com.web.appts.entities.ArchivedOrders;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchivedOrderRepo extends JpaRepository<ArchivedOrders, Long> {
  List<ArchivedOrders> findByUser(String paramString);

  List<ArchivedOrders> findByOrderNumber(String paramString);
}
