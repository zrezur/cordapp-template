package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.ApprovedTradingState;
import com.csg.oniontrading.contract.TradingState;
import com.csg.oniontrading.flow.services.Counterparties;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.KeyPair;

import static kotlin.collections.CollectionsKt.single;

public class BuyerApproveAndSendToRiskManager  extends FlowLogic<TradingFlowResult> {

    private String approverName = "someApproverInBankB";
    private TradingState tradingState;

    public BuyerApproveAndSendToRiskManager(TradingState tradingState) {
        this.tradingState = tradingState;
    }

    @Suspendable
    @Override
    public TradingFlowResult call() {
        final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
        final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

        ApprovedTradingState approvedTradingState = new ApprovedTradingState(approverName,
                tradingState.getSeller(),
                getServiceHub().getMyInfo().getLegalIdentity(),
                tradingState.getBuyer(),
                Counterparties.getRiskManagerParty(getServiceHub() ,tradingState.getBuyer()),
                tradingState.getContract());

        TransactionBuilder utx = approvedTradingState.generateAgreement(notary);
        utx.withItems(tradingState);
        utx.signWith(keyPair);

        SignedTransaction signedTransaction = utx.toSignedTransaction(false);

        Party riskManagerParty = Counterparties.getRiskManagerParty(getServiceHub(), tradingState.getBuyer());

        send(riskManagerParty, signedTransaction);

        return new TradingFlowResult.Success(String.format("Transaction id %s approved by Buyer send to to RiskManager.", signedTransaction.getId()));
    }
}
