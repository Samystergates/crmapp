package com.web.appts.entities;

import com.web.appts.utils.SpuOrderType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(
        name = "price_codes"
)
@Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE
)
public class PriceCodes {
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Column(
            name = "id"
    )
    private long id;
    @Column(
            name = "start_number"
    )
    private int startNumber;
    @Column(
            name = "end_number"
    )
    private int endNumber;

    private String velgmaat;

    private String code;

    private double prijs;

    private double energieToeslag;

    @Column(
            name = "spu_order_type"
    )
    @Enumerated(EnumType.STRING)
    private SpuOrderType spuOrderType;

    private String combinations;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }

    public int getEndNumber() {
        return endNumber;
    }

    public void setEndNumber(int endNumber) {
        this.endNumber = endNumber;
    }

    public String getVelgmaat() {
        return velgmaat;
    }

    public void setVelgmaat(String velgmaat) {
        this.velgmaat = velgmaat;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getPrijs() {
        return prijs;
    }

    public void setPrijs(double prijs) {
        this.prijs = prijs;
    }

    public double getEnergieToeslag() {
        return energieToeslag;
    }

    public void setEnergieToeslag(double energieToeslag) {
        this.energieToeslag = energieToeslag;
    }

    public SpuOrderType getSpuOrderType() {
        return spuOrderType;
    }

    public void setSpuOrderType(SpuOrderType spuOrderType) {
        this.spuOrderType = spuOrderType;
    }

    public String getCombinations() {
        return combinations;
    }

    public void setCombinations(String combinations) {
        this.combinations = combinations;
    }
}
