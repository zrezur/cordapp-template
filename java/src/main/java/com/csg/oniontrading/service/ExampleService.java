package com.csg.oniontrading.service;

import com.csg.oniontrading.flow.ExampleFlow;
import com.csg.oniontrading.flow.ExampleFlow;
import com.csg.oniontrading.flow.TradingFlow;
import kotlin.jvm.JvmClassMappingKt;
import net.corda.core.node.PluginServiceHub;

/**
 * This service registers a flow factory we wish to use when a initiating party attempts to communicate with us
 * using a particular flow. Registration is done against a marker class (in this case [ExampleFlow.IssueAndSendToRiskManager]
 * which is sent in the session handshake by the other party. If this marker class has been registered then the
 * corresponding factory will be used to create the flow which will communicate with the other side. If there is no
 * mapping then the session attempt is rejected.
 *
 * In short, this bit of code is required for the seller in this Example scenario to repond to the buyer using the
 * [ExampleFlow.Acceptor] flow.
 */
public class ExampleService {
    public ExampleService(PluginServiceHub services) {
        services.registerFlowInitiator(
                JvmClassMappingKt.getKotlinClass(TradingFlow.IssueAndSendToRiskManager.class),
                TradingFlow.RiskManagerTradingStore::new
        );
        services.registerFlowInitiator(
                JvmClassMappingKt.getKotlinClass(TradingFlow.RiskManagerApprove.class),
                TradingFlow.Acceptor::new
        );
    }
}