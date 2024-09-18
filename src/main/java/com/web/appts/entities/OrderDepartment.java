
package com.web.appts.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(
		name = "order_departments"
)
@Cache(
		usage = CacheConcurrencyStrategy.READ_WRITE
)
public class OrderDepartment {
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO
	)
	@Column(
			name = "id"
	)
	private long id;
	@Column(
			name = "dep_id"
	)
	private int depId;
	@Column(
			name = "dep_name"
	)
	private String depName;
	@Column(
			name = "dep_order_status"
	)
	private String status;
	@Column(
			name = "dep_order_prev_status"
	)
	private String prevStatus;
	@ManyToOne
	@JoinColumn(
			name = "order_id"
	)
	@JsonIgnore
	private Order order;

	public OrderDepartment() {
	}

	public OrderDepartment(int depId, String depName, String status, Order order) {
		this.depId = depId;
		this.depName = depName;
		this.status = status;
		this.order = order;
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getDepId() {
		return this.depId;
	}

	public void setDepId(int depId) {
		this.depId = depId;
	}

	public String getDepName() {
		return this.depName;
	}

	public void setDepName(String depName) {
		this.depName = depName;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Order getOrder() {
		return this.order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getPrevStatus() {
		return this.prevStatus;
	}

	public void setPrevStatus(String prevStatus) {
		this.prevStatus = prevStatus;
	}
}
