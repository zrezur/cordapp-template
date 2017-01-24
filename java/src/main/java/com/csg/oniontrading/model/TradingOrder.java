package com.csg.oniontrading.model;

import org.joda.time.Interval;

import java.util.UUID;

public class TradingOrder {
    private final String orderId;
    private String pair;
    private Integer nominal;
    private Interval tenor;

    public TradingOrder() {
        this.orderId = UUID.randomUUID().toString();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    public Integer getNominal() {
        return nominal;
    }

    public void setNominal(Integer nominal) {
        this.nominal = nominal;
    }

    public Interval getTenor() {
        return tenor;
    }

    public void setTenor(Interval tenor) {
        this.tenor = tenor;
    }
}
