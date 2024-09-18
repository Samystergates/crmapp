
package com.web.appts.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(
        name = "sticker_label"
)
public class StickerLabel {
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private long id;
    @Column(
            name = "organisatie"
    )
    private String organization;
    @Column(
            name = "Naam"
    )
    private String name;
    @Column(
            name = "produkt"
    )
    private String product;
    @Column(
            name = "extern_product"
    )
    private String externProduct;
    @Column(
            name = "omschrijving"
    )
    private String omsumin;
    @Column(
            name = "opmerking"
    )
    private String opmerking;
    @Column(
            name = "bandenmaat"
    )
    private String bandenmaat;
    @Column(
            name = "type"
    )
    private String type;
    @Column(
            name = "load_index"
    )
    private String loadIndex;
    @Column(
            name = "positie"
    )
    private String positie;
    @Column(
            name = "aansluitmaat"
    )
    private String aansluitmaat;
    @Column(
            name = "boutgat"
    )
    private String boutgat;
    @Column(
            name = "et"
    )
    private String et;
    @Column(
            name = "sjabloon"
    )
    private String sjabloon;
    @Column(
            name = "bar_code"
    )
    private String barCode;

    public StickerLabel() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBarCode() {
        return this.barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getOrganization() {
        return this.organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternProduct() {
        return this.externProduct;
    }

    public String getProduct() {
        return this.product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setExternProduct(String externProduct) {
        this.externProduct = externProduct;
    }

    public String getOmsumin() {
        return this.omsumin;
    }

    public void setOmsumin(String omsumin) {
        this.omsumin = omsumin;
    }

    public String getOpmerking() {
        return this.opmerking;
    }

    public void setOpmerking(String opmerking) {
        this.opmerking = opmerking;
    }

    public String getBandenmaat() {
        return this.bandenmaat;
    }

    public void setBandenmaat(String bandenmaat) {
        this.bandenmaat = bandenmaat;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLoadIndex() {
        return this.loadIndex;
    }

    public void setLoadIndex(String loadIndex) {
        this.loadIndex = loadIndex;
    }

    public String getPositie() {
        return this.positie;
    }

    public void setPositie(String positie) {
        this.positie = positie;
    }

    public String getAansluitmaat() {
        return this.aansluitmaat;
    }

    public void setAansluitmaat(String aansluitmaat) {
        this.aansluitmaat = aansluitmaat;
    }

    public String getBoutgat() {
        return this.boutgat;
    }

    public void setBoutgat(String boutgat) {
        this.boutgat = boutgat;
    }

    public String getEt() {
        return this.et;
    }

    public void setEt(String et) {
        this.et = et;
    }

    public String getSjabloon() {
        return this.sjabloon;
    }

    public void setSjabloon(String sjabloon) {
        this.sjabloon = sjabloon;
    }
}
