package com.web.appts.DTO;

import com.web.appts.entities.Order;


public class MonSubOrdersDto {

    private Long id;

    private String orderNumber;

    private String regel;

    private String aantal;

    private String product;

    private String Omsumin;

    private Order order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getRegel() {
        return regel;
    }

    public void setRegel(String regel) {
        this.regel = regel;
    }

    public String getAantal() {
        return aantal;
    }

    public void setAantal(String aantal) {
        this.aantal = aantal;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getOmsumin() {
        return Omsumin;
    }

    public void setOmsumin(String omsumin) {
        Omsumin = omsumin;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
