package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.ApprovedTradingState;
import com.csg.oniontrading.contract.TradingContract;
import com.csg.oniontrading.contract.TradingState;
import com.csg.oniontrading.flow.services.Counterparties;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.flows.FinalityFlow;

import java.security.KeyPair;
import java.security.SignatureException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static kotlin.collections.CollectionsKt.single;

public class SellerFinalize extends FlowLogic<TradingFlowResult> {

    private Party otherParty;

    public SellerFinalize(Party otherParty) {
        this.otherParty = otherParty;
    }

    @Suspendable
    @Override
    public TradingFlowResult call() {
        final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
        final SignedTransaction tx = this.receive(SignedTransaction.class, otherParty)
                .unwrap(data -> data);

        KeyPair keyPair = getServiceHub().getLegalIdentityKey();
        //check other signatures and original pojo
        TradingState tradingState = findState(TradingState.class, tx.getTx().getOutputs());
        ApprovedTradingState somePrevApprovedState = findState(ApprovedTradingState.class, tx.getTx().getOutputs());

        ApprovedTradingState approvedTradingState = new ApprovedTradingState(
                "automatic",
                somePrevApprovedState.getIssuer(),
                somePrevApprovedState.getIssuerApprover(),
                somePrevApprovedState.getBuyer(),
                Counterparties.getRiskManagerParty(getServiceHub(), somePrevApprovedState.getBuyer()),
                (TradingContract) somePrevApprovedState.getContract()
        );

        TransactionBuilder transactionBuilder = approvedTradingState.generateAgreement(notary);
        transactionBuilder.signWith(keyPair);
        SignedTransaction signedTransaction = transactionBuilder.toSignedTransaction(false);
        try {
            signedTransaction.checkSignaturesAreValid();
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }

//        DigitalSignature.WithKey signature = CryptoUtilities.signWithECDSA(keyPair, tx.getId().getBytes());

//        SignedTransaction signedTransaction = tx.withAdditionalSignature(signature);

        Set<Party> parties = new HashSet<Party>();
        parties.add(approvedTradingState.getIssuer());
        parties.add(approvedTradingState.getBuyer());
        subFlow(new FinalityFlow(signedTransaction, parties), false);
        return new TradingFlowResult.Success("Transaction confirmed.");
    }

    private <T> T findState(Class<T> clazz, List<TransactionState<ContractState>> outputs) {
        for (TransactionState<ContractState> output : outputs) {
            if(clazz.isInstance(output.getData())){
                return (T) output.getData();
            }
        }
        throw new RuntimeException("Can not find transactionState");
    }
}
