package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.util.Collections;

/**
 * Created by Y700-17 on 24.01.2017.
 */
public class BuyerRiskManagerTradingStore extends FlowLogic<TradingFlowResult> {

    private final Party issuer;

    public BuyerRiskManagerTradingStore(Party issuer) {
        this.issuer = issuer;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECORDING
    );

    private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
            "Recording transaction in vault.");

    @Suspendable
    @Override
    public TradingFlowResult call() {
        // Prep.
        // Obtain a reference to our key pair.
        final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

        // Stage 1.
        progressTracker.setCurrentStep(RECORDING);

        final SignedTransaction message = this.receive(SignedTransaction.class, issuer)
                .unwrap(data -> data);

        getServiceHub().recordTransactions(Collections.singletonList(message));
        return new TradingFlowResult.Success(String.format("Transaction id %s stored.", message.getId()));
    }
}
