
package com.web.appts.entities;

import java.util.List;
import javax.persistence.*;

@Entity
@Table(
		name = "archived_orders"
)
public class ArchivedOrders {
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
			name = "Ordersoort"
	)
	private String orderType;
	@Column(
			name = "Backorder"
	)
	private String backOrder;

	@Column(
			name = "cdProdGrp"
	)
	private String cdProdGrp;

	@Column(
			name = "zoeknaam"
	)
	private String zoeknaam;

	@Column(
			name = "SME"
	)
	private String sme;
	@Column(
			name = "SPU"
	)
	private String spu;
	@Column(
			name = "MON_LB"
	)
	private String monLb;
	@Column(
			name = "MON_TR"
	)
	private String monTr;
	@Column(
			name = "MWE"
	)
	private String mwe;
	@Column(
			name = "SER"
	)
	private String ser;
	@Column(
			name = "TRA"
	)
	private String tra;
	@Column(
			name = "EXP"
	)
	private String exp;
	@Column(
			name = "exclamation"
	)
	private String exclamation;
	@Column(
			name = "Gebruiker_I"
	)
	private String user;
	@Column(
			name = "Organisatie"
	)
	private String organization;
	@Column(
			name = "Naam"
	)
	private String customerName;
	@Column(
			name = "Straat"
	)
	private String street;
	@Column(
			name = "Huisnr"
	)
	private String houseNR;
	@Column(
			name = "Additioneel"
	)
	private String additionalAdd;
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
			name = "Referentie"
	)
	private String referenceInfo;
	@Column(
			name = "Datum_order"
	)
	private String creationDate;
	@Column(
			name = "Datum_laatste_wijziging"
	)
	private String modificationDate;
	@Column(
			name = "Gebruiker"
	)
	private String gebruikerRegel;

	@Column(name = "regel")
	private String verifierUser;

	@Column(name = "regel", insertable = false, updatable = false)
	private String regel;

	@Column(
			name = "Aantal"
	)
	private String aantal;
	@Column(
			name = "Aantal_geleverd"
	)
	private String gel;
	@Column(
			name = "Product"
	)
	private String product;
	@Column(
			name = "Omschrijving"
	)
	private String Omsumin;
	@Column(
			name = "is_expired"
	)
	private Boolean isExpired;
	@Column(
			name = "completed"
	)
	private String completed;
	@Column(
			name = "parent"
	)
	private int isParent;
	@OneToMany(
			fetch = FetchType.EAGER,
			mappedBy = "order",
			cascade = {CascadeType.ALL}
	)
	private List<OrderDepartment> Departments;

	public ArchivedOrders() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getIsParent() {
		return this.isParent;
	}

	public void setIsParent(int isParent) {
		this.isParent = isParent;
	}

	public String getOrderNumber() {
		return this.orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getOrderType() {
		return this.orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String isBackOrder() {
		return this.backOrder;
	}

	public void setBackOrder(String backOrder) {
		this.backOrder = backOrder;
	}

	public String getSme() {
		return this.sme;
	}

	public void setSme(String sme) {
		this.sme = sme;
	}

	public String getSpu() {
		return this.spu;
	}

	public void setSpu(String spu) {
		this.spu = spu;
	}

	public String getMonLb() {
		return this.monLb;
	}

	public void setMonLb(String monLb) {
		this.monLb = monLb;
	}

	public String getMonTr() {
		return this.monTr;
	}

	public void setMonTr(String monTr) {
		this.monTr = monTr;
	}

	public String getBackOrder() {
		return this.backOrder;
	}

	public String getMwe() {
		return this.mwe;
	}

	public void setMwe(String mwe) {
		this.mwe = mwe;
	}

	public String getSer() {
		return this.ser;
	}

	public void setSer(String ser) {
		this.ser = ser;
	}

	public String getTra() {
		return this.tra;
	}

	public void setTra(String tra) {
		this.tra = tra;
	}

	public String getExp() {
		return this.exp;
	}

	public void setExp(String exp) {
		this.exp = exp;
	}

	public String getExclamation() {
		return this.exclamation;
	}

	public void setExclamation(String exclamation) {
		this.exclamation = exclamation;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getOrganization() {
		return this.organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getCustomerName() {
		return this.customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getHouseNR() {
		return houseNR;
	}

	public void setHouseNR(String houseNR) {
		this.houseNR = houseNR;
	}

	public String getAdditionalAdd() {
		return additionalAdd;
	}

	public void setAdditionalAdd(String additionalAdd) {
		this.additionalAdd = additionalAdd;
	}

	public String getPostCode() {
		return this.postCode;
	}

	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return this.country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getDeliveryDate() {
		return this.deliveryDate;
	}

	public void setDeliveryDate(String deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public String getReferenceInfo() {
		return this.referenceInfo;
	}

	public void setReferenceInfo(String referenceInfo) {
		this.referenceInfo = referenceInfo;
	}

	public String getCreationDate() {
		return this.creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getModificationDate() {
		return this.modificationDate;
	}

	public void setModificationDate(String modificationDate) {
		this.modificationDate = modificationDate;
	}

	public String getVerifierUser() {
		return this.verifierUser;
	}

	public void setVerifierUser(String verifierUser) {
		this.verifierUser = verifierUser;
	}

	public Boolean getIsExpired() {
		return this.isExpired;
	}

	public void setIsExpired(Boolean isExpired) {
		this.isExpired = isExpired;
	}

	public String getRegel() {
		return this.regel;
	}

	public void setRegel(String regel) {
		this.regel = regel;
	}

	public String getAantal() {
		return this.aantal;
	}

	public void setAantal(String aantal) {
		this.aantal = aantal;
	}

	public String getGel() {
		return gel;
	}

	public void setGel(String gel) {
		this.gel = gel;
	}

	public String getProduct() {
		return this.product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getOmsumin() {
		return this.Omsumin;
	}

	public void setOmsumin(String omsumin) {
		this.Omsumin = omsumin;
	}

	public String getCompleted() {
		return this.completed;
	}

	public void setCompleted(String completed) {
		this.completed = completed;
	}

	public List<OrderDepartment> getDepartments() {
		return this.Departments;
	}

	public void setDepartments(List<OrderDepartment> departments) {
		this.Departments = departments;
	}

	public String getCdProdGrp() {
		return cdProdGrp;
	}

	public void setCdProdGrp(String cdProdGrp) {
		this.cdProdGrp = cdProdGrp;
	}

	public String getZoeknaam() {
		return zoeknaam;
	}

	public void setZoeknaam(String zoeknaam) {
		this.zoeknaam = zoeknaam;
	}

	public Boolean getExpired() {
		return isExpired;
	}

	public void setExpired(Boolean expired) {
		isExpired = expired;
	}
}
