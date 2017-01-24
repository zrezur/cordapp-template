package com.csg.oniontrading.contract;

import com.csg.oniontrading.model.TradingOrder;
import net.corda.core.contracts.*;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class TradingState implements DealState {
    private final TradingOrder tradingOrder;
    private final Party buyer;
    private final Party seller;
    private final TradingContract contract;
    private final UniqueIdentifier linearId;

    public TradingState(TradingOrder tradingOrder,
                              Party buyer,
                              Party seller,
                              TradingContract contract)
    {
        this.tradingOrder = tradingOrder;
        this.buyer = buyer;
        this.seller = seller;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(tradingOrder.getOrderId(), UUID.randomUUID());
    }

    public TradingOrder getPurchaseOrder() { return tradingOrder; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }
    @Override public TradingContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public String getRef() { return linearId.getExternalId(); }
    @Override public List<Party> getParties() { return Arrays.asList(buyer, seller); }
    @Override public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
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
    @Override public TransactionBuilder generateAgreement(Party notary) {
        return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new TradingContract.Commands.Trade(), getParticipants()));
    }
}
