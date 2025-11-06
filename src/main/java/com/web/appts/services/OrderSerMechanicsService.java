package com.web.appts.services;

import com.web.appts.DTO.OrderSERDto;

import java.util.List;

public interface OrderSerMechanicsService {
    OrderSERDto createOrderSER(OrderSERDto orderSERDto);

    OrderSERDto updateOrderSER(OrderSERDto orderSERDto);

    OrderSERDto getOrderSER(String orderNumber);

    Boolean deleteOrderSER(Long serId);

    List<OrderSERDto> getAllSer();

    List<String> getAllMonteurs();

    byte[] generateSERPdf(String key);
}
