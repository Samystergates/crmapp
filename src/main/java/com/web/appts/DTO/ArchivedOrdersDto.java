
package com.web.appts.DTO;

import com.web.appts.entities.OrderDepartment;

import javax.persistence.Column;
import java.util.List;

public class ArchivedOrdersDto {
    private long id;
    private String orderNumber;
    private String orderType;
    private String backOrder;
    private String sme;
    private String spu;
    private String monLb;
    private String monTr;
    private String mwe;
    private String ser;
    private String tra;
    private String exp;
    private String exclamation;
    private String user;
    private String organization;
    private String customerName;
    private String street;
    private String houseNR;
    private String additionalAdd;
    private String cdProdGrp;
    private String zoeknaam;
    private String postCode;
    private String city;
    private String country;
    private String deliveryDate;
    private String referenceInfo;
    private String creationDate;
    private String modificationDate;
    private String verifierUser;
    private Boolean isExpired;
    private String regel;
    private String aantal;
    private String product;
    private String Omsumin;
    private String completed;
    private List<OrderDepartment> departments;
    private int isParent;

    public ArchivedOrdersDto() {
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

    public String getBackOrder() {
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
        return this.departments;
    }

    public void setDepartments(List<OrderDepartment> departments) {
        this.departments = departments;
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
