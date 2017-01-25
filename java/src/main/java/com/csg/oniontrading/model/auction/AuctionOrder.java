package com.csg.oniontrading.model.auction;

import java.util.UUID;

public class AuctionOrder {
    private final String auctionId = UUID.randomUUID().toString();
    private String pair;
    private Integer nominal;
    private String tenor;
    private String auctionState;

    public AuctionOrder(String pair, Integer nominal, String tenor, String auctionState) {
        this.pair = pair;
        this.nominal = nominal;
        this.tenor = tenor;
        this.auctionState = auctionState;
    }

    public AuctionOrder() {
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

    public String getTenor() {
        return tenor;
    }

    public void setTenor(String tenor) {
        this.tenor = tenor;
    }

    public String getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(String auctionState) {
        this.auctionState = auctionState;
    }

    public String getAuctionId() {
        return auctionId;
    }
}
