package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.ApprovedTradingState;
import com.csg.oniontrading.contract.IssuerRiskManagerApprovedTradingState;
import com.csg.oniontrading.contract.TradingState;
import com.csg.oniontrading.flow.services.Counterparties;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;

import static kotlin.collections.CollectionsKt.single;

/**
 * Created by Y700-17 on 24.01.2017.
 */
public class IssuerRiskManagerApprove extends FlowLogic<TradingFlowResult> {

    //        private final Party issuer;
    private String approverName = "someApprover";
    private TradingState tradingState;

    public IssuerRiskManagerApprove(TradingState tradingState) {
//            this.issuer = issuer;
        this.tradingState = tradingState;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECORDING
    );

    private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
            "Recording transaction in vault.");

    @Suspendable
    @Override
    public TradingFlowResult call() {
        final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();

        // Prep.
        // Obtain a reference to our key pair.
        final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

        boolean isRisked = checkRisk(tradingState);
        if (!isRisked) {
            throw new RuntimeException("too high risk, don't trade");
        }

        ApprovedTradingState approvedTradingState = new ApprovedTradingState(approverName,
                tradingState.getSeller(),
                getServiceHub().getMyInfo().getLegalIdentity(),
                tradingState.getBuyer(),
                Counterparties.getRiskManagerParty(getServiceHub(), tradingState.getBuyer()),
                tradingState.getContract());

        TransactionBuilder utx = approvedTradingState.generateAgreement(notary);
        utx.withItems(tradingState);
        utx.signWith(keyPair);
        SignedTransaction signedTransaction = utx.toSignedTransaction(false);

        send(tradingState.getBuyer(), signedTransaction);

        return new TradingFlowResult.Success(String.format("Transaction id %s approved send to to buyer.", signedTransaction.getId()));
    }

    private boolean checkRisk(TradingState tradingState) {
        return true;
    }
}
