package com.web.appts.DTO;

public class OrderSERDto {

    private long id;

    private String orderNumber;

    private String monteur1;

    private String monteur2;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getMonteur1() {
        return monteur1;
    }

    public void setMonteur1(String monteur1) {
        this.monteur1 = monteur1;
    }

    public String getMonteur2() {
        return monteur2;
    }

    public void setMonteur2(String monteur2) {
        this.monteur2 = monteur2;
    }
}
