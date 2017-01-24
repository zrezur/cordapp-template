package com.csg.oniontrading.model;

import org.joda.time.Interval;

import java.util.UUID;

public class TradingOrder {

    private final String orderId;


    public TradingOrder() {
        this.orderId = UUID.randomUUID().toString();
    }

    public String getOrderId() {
        return orderId;
    }

    public static class ForwardOrder extends  TradingOrder {

        public Pair getPair() {
            return pair;
        }

        public void setPair(Pair pair) {
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

        private Pair pair;
        private Integer nominal;
        private Interval tenor;

        public  enum Pair {
            EURUSD,
            EURCHF,
            USDCHF;
        }
    }
}
