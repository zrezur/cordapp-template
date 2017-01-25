package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.auction.AuctionPostState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.util.Collections;

import static kotlin.collections.CollectionsKt.single;

public class StoreAuction extends FlowLogic<TradingFlowResult> {

    private AuctionPostState postState;
    private Party party;

    public StoreAuction(AuctionPostState postState) {
        this.postState = postState;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            SIGNING,
            RECORDING_AUCTION
    );

    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
            "Signing auction");
    private static final ProgressTracker.Step RECORDING_AUCTION = new ProgressTracker.Step(
            "Recording auction in vault.");

    public StoreAuction(AuctionPostState postState, Party party) {
        this.postState = postState;
        this.party = party;
    }



    @Suspendable
    @Override
    public TradingFlowResult call() {
        // Prep.
        // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered with
        // the NetWorkMapService. In a future milestone release we'll implement HD key generation such that new keys
        // can be generated for each transaction.
        final KeyPair myKeyPair = getServiceHub().getLegalIdentityKey();
        // Obtain a reference to the notary we want to use and its public key.
        final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
        final CompositeKey notaryPubKey = notary.getOwningKey();

        // Stage 1.
        progressTracker.setCurrentStep(SIGNING);
        // Construct a state object which encapsulates the PurchaseOrder object.
        // We add the public keys for us and the counterparty as well as a reference to the contract code.
        final TransactionState<AuctionPostState> auctionMessage = new TransactionState<AuctionPostState>(postState, notary);

        // Stage 2
        progressTracker.setCurrentStep(RECORDING_AUCTION);

        TransactionBuilder utx = auctionMessage.getData().generateAgreement(notary);

        final SignedTransaction stx = utx.signWith(myKeyPair).toSignedTransaction(false);

        getServiceHub().recordTransactions(Collections.singletonList(stx));
        return new TradingFlowResult.Success(String.format("Auction saved to store id: ", postState.getAuctionOrder().getAuctionId()));
    }
}
