package com.csg.oniontrading.model;

import java.util.UUID;

public class TradingOrder {

    private final String orderId;


    public TradingOrder() {
        this.orderId = UUID.randomUUID().toString();
    }

    public String getOrderId() {
        return orderId;
    }
}
