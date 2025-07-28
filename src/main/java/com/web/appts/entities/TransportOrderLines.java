package com.web.appts.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.appts.utils.OrderType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(
        name = "tra_order_lines"
)
@Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE
)
public class TransportOrderLines {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    private int orderRowNumber;
    private long orderId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_tra_id", referencedColumnName = "id")
    @JsonBackReference
//    @JsonIgnore
    private OrderTRA orderTra;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public int getOrderRowNumber() {
        return orderRowNumber;
    }

    public void setOrderRowNumber(int orderRowNumber) {
        this.orderRowNumber = orderRowNumber;
    }

    public OrderTRA getOrderTra() {
        return orderTra;
    }

    public void setOrderTra(OrderTRA orderTra) {
        this.orderTra = orderTra;
    }
}
