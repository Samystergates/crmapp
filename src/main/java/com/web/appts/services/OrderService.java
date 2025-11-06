
package com.web.appts.services;

import com.web.appts.DTO.CustomOrderDto;
import com.web.appts.DTO.DeleteCustOrderDto;
import com.web.appts.DTO.OrderDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface OrderService {
	Boolean createOrder(OrderDto paramOrderDto);

	CustomOrderDto createCustomOrder(CustomOrderDto customOrderDto);

	DeleteCustOrderDto deleteCustomOrder(CustomOrderDto customOrderDto);

	List<CustomOrderDto> getAllCustomOrders();

	Boolean archiveOrder(OrderDto paramOrderDto);

	List<OrderDto> updateOrder(OrderDto orderDto, Integer orderId, Boolean flowUpdate);

	List<OrderDto> updateOrderColors(String paramOrderNumber, String paramorderDep, String paramOrderStatus, String paramFlowVal);

	OrderDto getOrderById(Integer paramInteger);

	List<OrderDto> getOrdersByUser(String paramString);

	List<OrderDto> getAllOrders();

	void removingSameArchivedOrders();

	void checkOrderExistence();

	List<OrderDto> getCRMOrders();

	void createMonSubDemo();

	Map<String, OrderDto> createMonSub();

	public void adjustParentOrders();

	//@Transactional
	void updateProductNotes();

	Map<String, OrderDto> updateTextForOrders();

	List<OrderDto> updateAllTekst(String orderNumOrIds);

	void markExpired();

	Boolean updateTraColors(String ids, Long id);

	void deleteOrder(Integer paramInteger);

	public void generateExcelFile(OutputStream outputStream);

	List<OrderDto> checkMap();

	List<OrderDto> getOrdersByRegel(String regel);
}
