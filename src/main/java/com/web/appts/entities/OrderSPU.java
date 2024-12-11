
package com.web.appts.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(
		name = "order_spu"
)
@Cache(
		usage = CacheConcurrencyStrategy.READ_WRITE
)
public class OrderSPU {
	@Id
	@GeneratedValue(
			strategy = GenerationType.IDENTITY
	)
	@Column(
			name = "id"
	)
	private long id;
	@Column(
			name = "verkooporder"
	)
	private String orderNumber;
	@Column(
			name = "Artikelnummer"
	)
	private String prodNumber;
	@Column(
			name = "prijscode"
	)
	private String prijscode;
	@Column(
			name = "ralcode"
	)
	private String ralCode;
	@Column(
			name = "afdeling"
	)
	private String afdeling;
	@Column(
			name = "Kleurmschrijving"
	)
	private String kleurOmschrijving;
	@Column(
			name = "opmerking"
	)
	private String opmerking;
	@Column(
			name = "natlakken"
	)
	private String natLakken;
	@Column(
			name = "poedercoaten"
	)
	private String poedercoaten;
	@Column(
			name = "stralen"
	)
	private String stralen;
	@Column(
			name = "stralengedeeltelijk"
	)
	private String stralenGedeeltelijk;
	@Column(
			name = "schooperen"
	)
	private String schooperen;
	@Column(
			name = "kitten"
	)
	private String kitten;
	@Column(
			name = "primer"
	)
	private String primer;
	@Column(
			name = "ontlakken"
	)
	private String ontlakken;
	@Column(
			name = "aflakken"
	)
	private String aflakken;
	@Column(
			name = "blankelak"
	)
	private String blankeLak;

	@Column(
			name = "regel"
	)
	private String regel;

	public OrderSPU() {
	}

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

	public String getProdNumber() {
		return this.prodNumber;
	}

	public void setProdNumber(String prodNumber) {
		this.prodNumber = prodNumber;
	}

	public String getPrijscode() {
		return this.prijscode;
	}

	public void setPrijscode(String prijscode) {
		this.prijscode = prijscode;
	}

	public String getRalCode() {
		return this.ralCode;
	}

	public void setRalCode(String ralCode) {
		this.ralCode = ralCode;
	}

	public String getAfdeling() {
		return this.afdeling;
	}

	public void setAfdeling(String afdeling) {
		this.afdeling = afdeling;
	}

	public String getKleurOmschrijving() {
		return this.kleurOmschrijving;
	}

	public void setKleurOmschrijving(String kleurOmschrijving) {
		this.kleurOmschrijving = kleurOmschrijving;
	}

	public String getOpmerking() {
		return this.opmerking;
	}

	public void setOpmerking(String opmerking) {
		this.opmerking = opmerking;
	}

	public String getNatLakken() {
		return this.natLakken;
	}

	public void setNatLakken(String natLakken) {
		this.natLakken = natLakken;
	}

	public String getPoedercoaten() {
		return this.poedercoaten;
	}

	public void setPoedercoaten(String poedercoaten) {
		this.poedercoaten = poedercoaten;
	}

	public String getStralen() {
		return this.stralen;
	}

	public void setStralen(String stralen) {
		this.stralen = stralen;
	}

	public String getStralenGedeeltelijk() {
		return this.stralenGedeeltelijk;
	}

	public void setStralenGedeeltelijk(String stralenGedeeltelijk) {
		this.stralenGedeeltelijk = stralenGedeeltelijk;
	}

	public String getSchooperen() {
		return this.schooperen;
	}

	public void setSchooperen(String schooperen) {
		this.schooperen = schooperen;
	}

	public String getKitten() {
		return this.kitten;
	}

	public void setKitten(String kitten) {
		this.kitten = kitten;
	}

	public String getPrimer() {
		return this.primer;
	}

	public void setPrimer(String primer) {
		this.primer = primer;
	}

	public String getOntlakken() {
		return this.ontlakken;
	}

	public void setOntlakken(String ontlakken) {
		this.ontlakken = ontlakken;
	}

	public String getAflakken() {
		return this.aflakken;
	}

	public void setAflakken(String aflakken) {
		this.aflakken = aflakken;
	}

	public String getBlankeLak() {
		return this.blankeLak;
	}

	public void setBlankeLak(String blankeLak) {
		this.blankeLak = blankeLak;
	}

	public String getRegel() {
		return regel;
	}

	public void setRegel(String regel) {
		this.regel = regel;
	}
}
