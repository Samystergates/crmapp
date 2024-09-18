//package com.web.appts.entities;
//
//import javax.persistence.CascadeType;
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//import javax.persistence.JoinColumn;
//import javax.persistence.ManyToOne;
//import javax.persistence.Table;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//
//@Entity
//@Table(name = "order_deps")
//public class OrderDeps {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "id")
//	private int id;
//	@Column(name = "dep_id")
//	private int depId;
//	@Column(name = "dep_name")
//	private String depName;
//	@Column(name = "dep_order_status")
//	private String status;
//	@ManyToOne(cascade = CascadeType.PERSIST)
//	@JoinColumn(name = "order_id")
//	@JsonIgnore
//	private Order order;
//
//	public OrderDeps() {
//		super();
//		// TODO Auto-generated constructor stub
//	}
//
//	public OrderDeps(int depId, String depName, String status, Order order) {
//		this.depId = depId;
//		this.depName = depName;
//		this.status = status;
//		this.order = order;
//	}
//
//	public int getId() {
//		return this.id;
//	}
//
//	public void setId(int id) {
//		this.id = id;
//	}
//
//	public int getDepId() {
//		return depId;
//	}
//
//	public void setDepId(int depId) {
//		this.depId = depId;
//	}
//
//	public String getDepName() {
//		return depName;
//	}
//
//	public void setDepName(String depName) {
//		this.depName = depName;
//	}
//
//	public String getStatus() {
//		return this.status;
//	}
//
//	public void setStatus(String status) {
//		this.status = status;
//	}
//
//	public Order getOrder() {
//		return this.order;
//	}
//
//	public void setOrder(Order order) {
//		this.order = order;
//	}
//
//}