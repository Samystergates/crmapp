package com.web.appts.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(
        name = "mon_sub_orders",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"Verkooporder", "Regel", "Product"})
        }
)
@Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE
)
public class MonSubOrders {

    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO
    )
    @Column(
            name = "id"
    )
    private Long id;
    @Column(
            name = "Verkooporder"
    )
    private String orderNumber;
    @Column(
            name = "Regel"
    )
    private String regel;
    @Column(
            name = "Aantal"
    )
    private String aantal;
    @Column(
            name = "Product"
    )
    private String product;
    @Column(
            name = "Omschrijving"
    )
    private String Omsumin;
    @ManyToOne
    @JoinColumn(
            name = "order_id"
    )
    @JsonIgnore
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
