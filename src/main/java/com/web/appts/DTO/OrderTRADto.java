
package com.web.appts.DTO;

import com.web.appts.entities.TransportOrderLines;

import java.util.List;

public class OrderTRADto {
	private long id;
	private String routeDate;
	private String route;
	private String chauffeur;
	private String truck;
	private String trailer;
	private List<TransportOrderLinesDto> orderIds;

	private Boolean isCompleted;

	public OrderTRADto() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getRouteDate() {
		return this.routeDate;
	}

	public void setRouteDate(String routeDate) {
		this.routeDate = routeDate;
	}

	public String getRoute() {
		return this.route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getChauffeur() {
		return this.chauffeur;
	}

	public void setChauffeur(String chauffeur) {
		this.chauffeur = chauffeur;
	}

	public String getTruck() {
		return this.truck;
	}

	public void setTruck(String truck) {
		this.truck = truck;
	}

	public String getTrailer() {
		return this.trailer;
	}

	public void setTrailer(String trailer) {
		this.trailer = trailer;
	}

	public List<TransportOrderLinesDto> getOrderIds() {
		return orderIds;
	}

	public void setOrderIds(List<TransportOrderLinesDto> orderIds) {
		this.orderIds = orderIds;
	}

	public Boolean getCompleted() {
		return isCompleted;
	}

	public void setCompleted(Boolean completed) {
		isCompleted = completed;
	}

	public Boolean getIsCompleted() {
		return this.isCompleted;
	}

	public void setIsCompleted(Boolean isCompleted) {
		this.isCompleted = isCompleted;
	}
}
