
package com.web.appts.services;

import com.web.appts.DTO.OrderDto;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface OrderService {
	Boolean createOrder(OrderDto paramOrderDto);

	Boolean archiveOrder(OrderDto paramOrderDto);

	List<OrderDto> updateOrder(OrderDto paramOrderDto, Integer paramInteger, Boolean paramBoolean);

	List<OrderDto> updateOrderColors(String paramOrderNumber, String paramorderDep, String paramOrderStatus, String paramFlowVal);

	OrderDto getOrderById(Integer paramInteger);

	List<OrderDto> getOrdersByUser(String paramString);

	List<OrderDto> getAllOrders();

	void removingSameArchivedOrders();

	List<OrderDto> getCRMOrders();

	void createMonSubDemo();

	Map<String, OrderDto> createMonSub();

	public void adjustParentOrders();

	//@Transactional
	void updateProductNotes();

	Map<String, OrderDto> updateTextForOrders();

	void markExpired();

	Boolean updateTraColors(String ids, Long id);

	void deleteOrder(Integer paramInteger);

	public void generateExcelFile(OutputStream outputStream);

	List<OrderDto> checkMap();
}
