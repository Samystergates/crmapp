package com.web.appts.DTO;

import com.web.appts.entities.TransportOrderLines;

public class DeleteCustOrderDto {

    private TransportOrderLines transportOrderLines;
    private Boolean result;

    public DeleteCustOrderDto() {
    }

    public DeleteCustOrderDto(TransportOrderLines transportOrderLines, Boolean result) {
        this.transportOrderLines = transportOrderLines;
        this.result = result;
    }

    public TransportOrderLines getTransportOrderLines() {
        return transportOrderLines;
    }

    public void setTransportOrderLines(TransportOrderLines transportOrderLines) {
        this.transportOrderLines = transportOrderLines;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
}
