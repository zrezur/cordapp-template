package com.csg.oniontrading.flow;

/**
 * Created by Y700-17 on 24.01.2017.
 */
public class TradingFlowResult {
    public static class Success extends TradingFlowResult {
        private String message;

        public Success(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("Success(%s)", message);
        }
    }

    public static class Failure extends TradingFlowResult {
        private String message;

        public Failure(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("Failure(%s)", message);
        }
    }
}
