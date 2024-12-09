
package com.web.appts.services;

import com.web.appts.DTO.ArchivedOrdersDto;
import com.web.appts.DTO.OrderDto;
import java.util.List;

public interface ArchivedOrdersService {
  Boolean createArchivedOrder(OrderDto paramOrderDto);

  ArchivedOrdersDto getArchivedOrderById(Long paramInteger);

  List<ArchivedOrdersDto> getArchivedOrdersByUser(String paramString);

  List<ArchivedOrdersDto> getAllArchivedOrders();

  void validateArchiveMap();

  List<ArchivedOrdersDto> getArchivedOrdersByRegel(String regel);
}
