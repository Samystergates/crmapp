
package com.web.appts.services;

import com.web.appts.DTO.OrderDto;
import java.util.List;

public interface OrderService {
	Boolean createOrder(OrderDto paramOrderDto);

	Boolean archiveOrder(OrderDto paramOrderDto);

	List<OrderDto> updateOrder(OrderDto paramOrderDto, Integer paramInteger, Boolean paramBoolean);

	List<OrderDto> updateOrderColors(String paramOrderNumber, String paramorderDep, String paramOrderStatus, String paramFlowVal);

	OrderDto getOrderById(Integer paramInteger);

	List<OrderDto> getOrdersByUser(String paramString);

	List<OrderDto> getAllOrders();

	List<OrderDto> getCRMOrders();

	Boolean updateTraColors(String ids, Long id);

	void deleteOrder(Integer paramInteger);

	List<OrderDto> checkMap();

	List<OrderDto> getOrdersByRegel(String regel);
}
