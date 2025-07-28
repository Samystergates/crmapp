package com.web.appts.DTO;

import com.web.appts.entities.OrderTRA;
import com.web.appts.utils.OrderType;

public class TransportOrderLinesDto {

    private Long id;
    private OrderType orderType;
    private int orderRowNumber;
    private long orderId;
//    private OrderTRADto orderTraDto;
//
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getOrderRowNumber() {
        return orderRowNumber;
    }

    public void setOrderRowNumber(int orderRowNumber) {
        this.orderRowNumber = orderRowNumber;
    }

//    public OrderTRADto getOrderTraDto() {
//        return orderTraDto;
//    }
//
//    public void setOrderTraDto(OrderTRADto orderTraDto) {
//        this.orderTraDto = orderTraDto;
//    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
}
