package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.TradingState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.utilities.ProgressTracker;
import static kotlin.collections.CollectionsKt.single;

/**
 * Created by Y700-17 on 24.01.2017.
 */
public class IssueAndSendToRiskManager extends FlowLogic<TradingFlowResult> {

    private final TradingState tradingState;
    private final Party otherParty;
    // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
    // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
    // function.
    private final ProgressTracker progressTracker = new ProgressTracker(
            CONSTRUCTING_OFFER,
            APPROVAL,
            SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION,
            VERIFYING,
            SIGNING,
            NOTARY,
            RECORDING,
            SENDING_FINAL_TRANSACTION
    );

    private static final ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step(
            "Constructing proposed purchase order.");
    private static final ProgressTracker.Step APPROVAL = new ProgressTracker.Step(
            "Offer approval by risk manager.");
    private static final ProgressTracker.Step SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION = new ProgressTracker.Step(
            "Sending purchase order to seller for review, and receiving partially signed transaction from seller in return.");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step(
            "Verifying signatures and contract constraints.");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
            "Signing transaction with our private key.");
    private static final ProgressTracker.Step NOTARY = new ProgressTracker.Step(
            "Obtaining notary signature.");
    private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
            "Recording transaction in vault.");
    private static final ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step(
            "Sending fully signed transaction to seller.");

    public IssueAndSendToRiskManager(TradingState tradingState, Party otherParty) {
        this.tradingState = tradingState;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public TradingFlowResult call() {
        try {
            final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();

            // Stage 1.
            progressTracker.setCurrentStep(CONSTRUCTING_OFFER);
            // Construct a state object which encapsulates the PurchaseOrder object.
            // We add the public keys for us and the counterparty as well as a reference to the contract code.
            final TransactionState offerMessage = new TransactionState<ContractState>(tradingState, notary);

            // Stage 2.
            progressTracker.setCurrentStep(APPROVAL);
            Party riskManagerParty = getRiskManagerParty(getServiceHub().getMyInfo().getLegalIdentity());
            send(riskManagerParty, offerMessage);

            // Stage 3.
            return new TradingFlowResult.Success(String.format("Transaction passed to Risk manager."));
        } catch (Exception ex) {
            // Just catch all exception types.
            return new TradingFlowResult.Failure(ex.getMessage());
        }
    }

    public Party getRiskManagerParty(Party source) {
        if (source.getName().equalsIgnoreCase(TradingFlow.NAME_NODE_A)) {

            return getServiceHub().getNetworkMapCache().getNodeByLegalName(TradingFlow.NAME_RISK_MANAGER_A).getLegalIdentity();
        } else if (source.getName().equalsIgnoreCase(TradingFlow.NAME_NODE_B)) {
            return getServiceHub().getNetworkMapCache().getNodeByLegalName(TradingFlow.NAME_RISK_MANAGER_B).getLegalIdentity();
        } else {
            throw new RuntimeException("Unknown party " + source.getName());
        }
    }
}
