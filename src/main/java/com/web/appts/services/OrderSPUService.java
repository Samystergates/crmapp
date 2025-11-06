
package com.web.appts.services;

import com.web.appts.DTO.OrderSPUDto;
import com.web.appts.DTO.PriceCodesDto;
import com.web.appts.DTO.SpuDepartmentsDto;

import java.util.List;

public interface OrderSPUService {
	OrderSPUDto createOrderSPU(OrderSPUDto orderSPUDto);

	OrderSPUDto updateOrderSPU(OrderSPUDto orderSPUDto);

	Boolean deleteOrderSPU(Long orderSPUId);

	OrderSPUDto getOrderSPU(String orderNumber, String regel);

	List<OrderSPUDto> getAllSpu();

	List<PriceCodesDto> getAllPriceCodes();

	List<SpuDepartmentsDto> getAllSpuDepartments();

	byte[] generateSPUPdf(String key);
}
