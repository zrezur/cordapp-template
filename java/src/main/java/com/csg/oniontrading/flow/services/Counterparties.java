package com.csg.oniontrading.flow.services;


import com.csg.oniontrading.flow.TradingFlow;
import net.corda.core.crypto.Party;
import net.corda.core.node.ServiceHub;

public class Counterparties {


    static public Party getRiskManagerParty(ServiceHub serviceHub, Party source) {
        if (source.getName().equalsIgnoreCase(TradingFlow.NAME_NODE_A)) {

            return serviceHub.getNetworkMapCache().getNodeByLegalName(TradingFlow.NAME_RISK_MANAGER_A).getLegalIdentity();
        } else if (source.getName().equalsIgnoreCase(TradingFlow.NAME_NODE_B)) {
            return serviceHub.getNetworkMapCache().getNodeByLegalName(TradingFlow.NAME_RISK_MANAGER_B).getLegalIdentity();
        } else {
            throw new RuntimeException("Unknown party " + source.getName());
        }
    }
}
