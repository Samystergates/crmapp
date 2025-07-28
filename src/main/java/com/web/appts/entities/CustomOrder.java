package com.web.appts.entities;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(
        name = "custom_orders"
)
@Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE
)
public class CustomOrder {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private long id;

    @Column(
            name = "Verkooporder"
    )
    private String orderNumber;
    @Column(
            name = "Naam"
    )
    private String customerName;
    @Column(
            name = "Postcode"
    )
    private String postCode;
    @Column(
            name = "Plaats"
    )
    private String city;
    @Column(
            name = "Land"
    )
    private String country;
    @Column(
            name = "Leverdatum"
    )
    private String deliveryDate;
    @Column(
            name = "opmerking"
    )
    private String opmerking;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getOpmerking() {
        return opmerking;
    }

    public void setOpmerking(String opmerking) {
        this.opmerking = opmerking;
    }
}
