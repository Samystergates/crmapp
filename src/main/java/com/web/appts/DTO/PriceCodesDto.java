package com.web.appts.DTO;

import com.web.appts.utils.SpuOrderType;

public class PriceCodesDto {

    private long id;

    private int startNumber;

    private int endNumber;

    private String velgmaat;

    private String code;

    private double prijs;

    private double energieToeslag;

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
