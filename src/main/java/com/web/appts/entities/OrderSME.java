
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
		name = "order_sme"
)
@Cache(
		usage = CacheConcurrencyStrategy.READ_WRITE
)
public class OrderSME {
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
			name = "naafgat"
	)
	private String naafgat;
	@Column(
			name = "skeedcirkel"
	)
	private String steek;
	@Column(
			name = "aantalbout"
	)
	private String aantalBoutgat;
	@Column(
			name = "verdelingbout"
	)
	private String verdlingBoutgaten;
	@Column(
			name = "diameterbout"
	)
	private String diameter;
	@Column(
			name = "uitvoeringbout"
	)
	private String typeBoutgat;
	@Column(
			name = "maatverzinking"
	)
	private String maatVerzinking;
	@Column(
			name = "ET"
	)
	private String et;
	@Column(
			name = "afstandv"
	)
	private String afstandVV;
	@Column(
			name = "afstandach"
	)
	private String afstandVA;
	@Column(
			name = "dikte"
	)
	private String dikte;
	@Column(
			name = "opmerking"
	)
	private String opmerking;
	@Column(
			name = "merk"
	)
	private String merk;
	@Column(
			name = "model"
	)
	private String model;
	@Column(
			name = "type"
	)
	private String type;
	@Column(
			name = "doorgezet"
	)
	private String doorgezet;
	@Column(
			name = "koelgaten"
	)
	private String koelgaten;
	@Column(
			name = "verstevigingsringen"
	)
	private String verstevigingsringen;
	@Column(
			name = "ventielbeschermer"
	)
	private String ventielbeschermer;
	@Column(
			name = "aansluitnippel "
	)
	private String aansluitnippel;

	public OrderSME() {
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

	public String getNaafgat() {
		return this.naafgat;
	}

	public void setNaafgat(String naafgat) {
		this.naafgat = naafgat;
	}

	public String getMaatVerzinking() {
		return this.maatVerzinking;
	}

	public void setMaatVerzinking(String maatVerzinking) {
		this.maatVerzinking = maatVerzinking;
	}

	public String getEt() {
		return this.et;
	}

	public void setEt(String et) {
		this.et = et;
	}

	public String getSteek() {
		return this.steek;
	}

	public void setSteek(String steek) {
		this.steek = steek;
	}

	public String getAantalBoutgat() {
		return this.aantalBoutgat;
	}

	public void setAantalBoutgat(String aantalBoutgat) {
		this.aantalBoutgat = aantalBoutgat;
	}

	public String getVerdlingBoutgaten() {
		return this.verdlingBoutgaten;
	}

	public void setVerdlingBoutgaten(String verdlingBoutgaten) {
		this.verdlingBoutgaten = verdlingBoutgaten;
	}

	public String getDiameter() {
		return this.diameter;
	}

	public void setDiameter(String diameter) {
		this.diameter = diameter;
	}

	public String getTypeBoutgat() {
		return this.typeBoutgat;
	}

	public void setTypeBoutgat(String typeBoutgat) {
		this.typeBoutgat = typeBoutgat;
	}

	public String getAfstandVV() {
		return this.afstandVV;
	}

	public void setAfstandVV(String afstandVV) {
		this.afstandVV = afstandVV;
	}

	public String getAfstandVA() {
		return this.afstandVA;
	}

	public void setAfstandVA(String afstandVA) {
		this.afstandVA = afstandVA;
	}

	public String getDikte() {
		return this.dikte;
	}

	public void setDikte(String dikte) {
		this.dikte = dikte;
	}

	public String getOpmerking() {
		return this.opmerking;
	}

	public void setOpmerking(String opmerking) {
		this.opmerking = opmerking;
	}

	public String getMerk() {
		return this.merk;
	}

	public void setMerk(String merk) {
		this.merk = merk;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDoorgezet() {
		return this.doorgezet;
	}

	public void setDoorgezet(String doorgezet) {
		this.doorgezet = doorgezet;
	}

	public String getKoelgaten() {
		return this.koelgaten;
	}

	public void setKoelgaten(String koelgaten) {
		this.koelgaten = koelgaten;
	}

	public String getVerstevigingsringen() {
		return this.verstevigingsringen;
	}

	public void setVerstevigingsringen(String verstevigingsringen) {
		this.verstevigingsringen = verstevigingsringen;
	}

	public String getVentielbeschermer() {
		return this.ventielbeschermer;
	}

	public void setVentielbeschermer(String ventielbeschermer) {
		this.ventielbeschermer = ventielbeschermer;
	}

	public String getAansluitnippel() {
		return this.aansluitnippel;
	}

	public void setAansluitnippel(String aansluitnippel) {
		this.aansluitnippel = aansluitnippel;
	}
}
