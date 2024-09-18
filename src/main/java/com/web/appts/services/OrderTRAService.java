
package com.web.appts.services;

import com.web.appts.DTO.OrderTRADto;
import java.util.Map;

public interface OrderTRAService {
	OrderTRADto createOrderTRA(OrderTRADto orderTRADto);

	OrderTRADto updateOrderTRA(OrderTRADto orderTRADto);

	Boolean deleteOrderTRA(Long orderTRAId);

	OrderTRADto getOrderTRA(Long orderTRAId);

	Boolean updateOrderTRAColors(String orderTRAIds, Long Id);

	Map<String, OrderTRADto> getAllTraOrders();

	byte[] generateTRAPdf(OrderTRADto orderTRADto);
}
