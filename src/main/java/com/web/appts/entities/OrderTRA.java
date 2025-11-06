
package com.web.appts.entities;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.List;

@Entity
@Table(
		name = "order_tra"
)
@Cache(
		usage = CacheConcurrencyStrategy.READ_WRITE
)
public class OrderTRA {
	@Id
	@GeneratedValue(
			strategy = GenerationType.IDENTITY
	)
	@Column(
			name = "id"
	)
	private long id;
	@Column(
			name = "route_date"
	)
	private String routeDate;
	private String route;
	private String chauffeur;
	private String truck;
	private String trailer;
	@OneToMany(mappedBy = "orderTra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<TransportOrderLines> orderIds;
	private Boolean isCompleted;

	public OrderTRA() {
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

	public List<TransportOrderLines> getOrderIds() {
		return orderIds;
	}

	public void setOrderIds(List<TransportOrderLines> orderIds) {
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
