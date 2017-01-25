package com.csg.oniontrading.contract.auction;

import com.csg.oniontrading.model.auction.AuctionOrder;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionType;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class AuctionPostState implements DealState {
    private final AuctionOrder auctionOrder;
    private final Party poster;
    private final AuctionPostContract contract;
    private final UniqueIdentifier linearId;

    public AuctionPostState(AuctionOrder auctionOrder,
                            Party poster,
                            AuctionPostContract contract
    ) {
        this.auctionOrder = auctionOrder;
        this.poster = poster;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(auctionOrder.getAuctionId(), UUID.randomUUID());
    }

    public Party getPoster() {
        return poster;
    }

    @Override
    public AuctionPostContract getContract() {
        return contract;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public String getRef() {
        return linearId.getExternalId();
    }

    @Override
    public List<Party> getParties() {
        return Arrays.asList(poster);
    }

    @Override
    public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    public AuctionOrder getAuctionOrder() {
        return auctionOrder;
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override
    public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final List<PublicKey> partyKeys = getParties()
                .stream()
                .flatMap(party -> party.getOwningKey().getKeys().stream())
                .collect(toList());
        return ourKeys
                .stream()
                .anyMatch(partyKeys::contains);

    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     */
    @Override
    public TransactionBuilder generateAgreement(Party notary) {
        return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new AuctionPostContract.Commands.Post(), getParticipants()));
    }

}
