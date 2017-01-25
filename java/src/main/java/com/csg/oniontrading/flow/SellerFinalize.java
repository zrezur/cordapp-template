package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.flows.FinalityFlow;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;

public class SellerFinalize extends FlowLogic<TradingFlowResult> {

    private Party otherParty;

    public SellerFinalize(Party otherParty) {
        this.otherParty = otherParty;
    }

    @Suspendable
    @Override
    public TradingFlowResult call() {
        final SignedTransaction tx = this.receive(SignedTransaction.class, otherParty)
                .unwrap(data -> data);

        KeyPair keyPair = getServiceHub().getLegalIdentityKey();
        //check other signatures and original pojo

        DigitalSignature.WithKey signature = CryptoUtilities.signWithECDSA(keyPair, tx.getId().getBytes());

        SignedTransaction signedTransaction = tx.withAdditionalSignature(signature);

        Set<Party> parties = new HashSet<>();
        subFlow(new FinalityFlow(signedTransaction, parties), false);
        return new TradingFlowResult.Success("Transaction confirmed.");
    }
}
