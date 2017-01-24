package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.ApprovedTradingState;
import com.csg.oniontrading.contract.TradingState;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.util.Collections;

import static kotlin.collections.CollectionsKt.single;

/**
 * This is the "Hello World" of flows!
 *
 * It is a generic flow which facilitates the workflow required for two parties; an [IssueAndSendToRiskManager] and an [BuyerStore],
 * to come to an agreement about some arbitrary data (in this case, a [PurchaseOrder]) encapsulated within a [DealState].
 *
 * As this is just an example there's no way to handle any counter-proposals. The [BuyerStore] always accepts the
 * proposed state assuming it satisfies the referenced [Contract]'s issuance constraints.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * NB. All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * The flows below have been heavily commented to aid your understanding. It may also be worth reading the CorDapp
 * tutorial documentation on the Corda docsite (https://docs.corda.net) which includes a sequence diagram which clearly
 * explains each stage of the flow.
 */
public class TradingFlow {

    public static final String NAME_NODE_A = "NodeA";
    public static final String NAME_NODE_B = "NodeB";
    public static final String NAME_RISK_MANAGER_A = "RiskManagerA";
    public static final String NAME_RISK_MANAGER_B = "RiskManagerB";



}
