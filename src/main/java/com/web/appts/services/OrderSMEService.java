
package com.web.appts.services;

import com.web.appts.DTO.OrderSMEDto;
import java.util.List;

public interface OrderSMEService {
	OrderSMEDto createOrderSME(OrderSMEDto orderSMEDto);

	OrderSMEDto updateOrderSME(OrderSMEDto orderSMEDto);

	Boolean deleteOrderSME(Long orderSMEId);

	OrderSMEDto getOrderSME(String orderNumber, String prodNumber);

	List<OrderSMEDto> getAllSme();

	byte[] generateSMEPdf(String key);
}
