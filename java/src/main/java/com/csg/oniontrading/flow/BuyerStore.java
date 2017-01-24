package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.util.Collections;

public class BuyerStore extends FlowLogic<TradingFlowResult> {

    private final Party otherParty;
    private final ProgressTracker progressTracker = new ProgressTracker(
            WAIT_FOR_AND_RECEIVE_PROPOSAL,
            GENERATING_TRANSACTION,
            SIGNING,
            SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
            VERIFYING_TRANSACTION,
            RECORDING
    );

    private static final ProgressTracker.Step WAIT_FOR_AND_RECEIVE_PROPOSAL = new ProgressTracker.Step(
            "Receiving proposed purchase order from buyer.");
    private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
            "Generating transaction based on proposed purchase order.");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
            "Signing proposed transaction with our private key.");
    private static final ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step(
            "Sending partially signed transaction to buyer and wait for a response.");
    private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
            "Verifying signatures and contract constraints.");
    private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
            "Recording transaction in vault.");

    public BuyerStore(Party otherParty) {
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public TradingFlowResult call() {
        try {
            // Prep.
            // Obtain a reference to our key pair.
            final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

            // Stage 3.
            progressTracker.setCurrentStep(WAIT_FOR_AND_RECEIVE_PROPOSAL);
            // All messages come off the wire as UntrustworthyData. You need to 'unwrap' it. This is an appropriate
            // place to perform some validation over what you have just received.
            final SignedTransaction message = this.receive(SignedTransaction.class, otherParty)
                    .unwrap(data -> data);
//
//                StateRef stateRef = message.getTx().getInputs().get(0);
//                TransactionState<?> transactionState = getServiceHub().loadState(stateRef);
//                System.out.println(transactionState);
//                DealState data = message.getData();
//                System.out.println(data);

            getServiceHub().recordTransactions(Collections.singleton(message));
            return new TradingFlowResult.Success(String.format("Transaction %s stored to approve.", message.getId()));
        } catch (Exception ex) {
            return new TradingFlowResult.Failure(ex.getMessage());
        }
    }
}
