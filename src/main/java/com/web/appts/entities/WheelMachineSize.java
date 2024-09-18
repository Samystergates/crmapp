
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
		name = "wheel_machine_size"
)
@Cache(
		usage = CacheConcurrencyStrategy.READ_WRITE
)
public class WheelMachineSize {
	@Id
	@GeneratedValue(
			strategy = GenerationType.IDENTITY
	)
	@Column(
			name = "id"
	)
	private long id;
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
			name = "bandenmaat"
	)
	private String bandenmaat;
	@Column(
			name = "velgmaat"
	)
	private String velgmaat;
	@Column(
			name = "velguitvoering"
	)
	private String velgtype;
	@Column(
			name = "velgnummer"
	)
	private String velgnummer;
	@Column(
			name = "velgleverancier"
	)
	private String velgleverancier;
	@Column(
			name = "Afdeling"
	)
	private String department;
	@Column(
			name = "naafgat"
	)
	private String naafgat;
	@Column(
			name = "steekcirkel"
	)
	private String steek;
	@Column(
			name = "aantalboutgaten"
	)
	private String aantalBoutgat;
	@Column(
			name = "verdlingboutgaten"
	)
	private String verdlingBoutgaten;
	@Column(
			name = "diameterboutgat"
	)
	private String diameter;
	@Column(
			name = "uitvoeringboutgat"
	)
	private String typeBoutgat;
	@Column(
			name = "maatverzinking"
	)
	private String maatVerzinking;
	@Column(
			name = "offset"
	)
	private String et;
	@Column(
			name = "afstandvoorzijde"
	)
	private String afstandVV;
	@Column(
			name = "afstandachterzijde"
	)
	private String afstandVA;
	@Column(
			name = "flensuitvoering"
	)
	private String uitvoerechtingFlens;
	@Column(
			name = "dikteschijf"
	)
	private String dikte;
	@Column(
			name = "koelgaten"
	)
	private String koelgaten;
	@Column(
			name = "opmerking"
	)
	private String opmerking;

	public WheelMachineSize() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getBandenmaat() {
		return this.bandenmaat;
	}

	public void setBandenmaat(String bandenmaat) {
		this.bandenmaat = bandenmaat;
	}

	public String getVelgmaat() {
		return this.velgmaat;
	}

	public void setVelgmaat(String velgmaat) {
		this.velgmaat = velgmaat;
	}

	public String getVelgtype() {
		return this.velgtype;
	}

	public void setVelgtype(String velgtype) {
		this.velgtype = velgtype;
	}

	public String getVelgnummer() {
		return this.velgnummer;
	}

	public void setVelgnummer(String velgnummer) {
		this.velgnummer = velgnummer;
	}

	public String getVelgleverancier() {
		return this.velgleverancier;
	}

	public void setVelgleverancier(String velgleverancier) {
		this.velgleverancier = velgleverancier;
	}

	public String getDepartment() {
		return this.department;
	}

	public void setDepartment(String department) {
		this.department = department;
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

	public String getEt() {
		return this.et;
	}

	public void setEt(String et) {
		this.et = et;
	}

	public String getUitvoerechtingFlens() {
		return this.uitvoerechtingFlens;
	}

	public void setUitvoerechtingFlens(String uitvoerechtingFlens) {
		this.uitvoerechtingFlens = uitvoerechtingFlens;
	}

	public String getKoelgaten() {
		return this.koelgaten;
	}

	public void setKoelgaten(String koelgaten) {
		this.koelgaten = koelgaten;
	}

	public String getOpmerking() {
		return this.opmerking;
	}

	public void setOpmerking(String opmerking) {
		this.opmerking = opmerking;
	}

	public String getNaafgat() {
		return this.naafgat;
	}

	public void setNaafgat(String naafgat) {
		this.naafgat = naafgat;
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

	public String getMaatVerzinking() {
		return this.maatVerzinking;
	}

	public void setMaatVerzinking(String maatVerzinking) {
		this.maatVerzinking = maatVerzinking;
	}
}
