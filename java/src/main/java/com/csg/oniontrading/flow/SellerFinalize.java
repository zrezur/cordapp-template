package com.csg.oniontrading.flow;

import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;

public class SellerFinalize extends FlowLogic<TradingFlowResult> {

    private Party seller;

    public SellerFinalize(Party seller) {
        this.seller = seller;
    }

    @Override
    public TradingFlowResult call() {
        return null;
    }
}
