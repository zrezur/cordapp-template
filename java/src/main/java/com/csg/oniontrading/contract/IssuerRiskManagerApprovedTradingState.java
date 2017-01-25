package com.csg.oniontrading.contract;


import net.corda.core.contracts.*;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class IssuerRiskManagerApprovedTradingState implements DealState{

    private String approverName;
    private final Party issuer;
    private final Party issuerApprover;
    private final Party buyerApprover;
    private final Party buyer;
    private UniqueIdentifier linearId;
    private final TradingContract contract;

    public IssuerRiskManagerApprovedTradingState(String approverName, Party issuer, Party approver, Party buyer, Party buyerApprover, TradingContract contract) {
        this.approverName = approverName;
        this.issuer = issuer;
        this.buyer = buyer;
        this.issuerApprover = approver;
        this.buyerApprover = buyerApprover;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(UUID.randomUUID().toString(), UUID.randomUUID());
    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     */
    @Override public TransactionBuilder generateAgreement(Party notary) {
        return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new TradingContract.Commands.Approve(), getParticipants()));
    }

    @Override public List<Party> getParties() { return Arrays.asList(issuerApprover); }

    @Override public String getRef() { return linearId.getExternalId(); }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public boolean isRelevant(Set<? extends PublicKey> set) {
        final List<PublicKey> partyKeys = Arrays.asList(issuerApprover)
                .stream()
                .flatMap(party -> party.getOwningKey().getKeys().stream())
                .collect(toList());
        return set
                .stream()
                .anyMatch(partyKeys::contains);
    }

    @NotNull
    @Override
    public Contract getContract() {
        return contract;
    }

    @NotNull
    @Override
    public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    public String getApproverName() {
        return approverName;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getIssuerApprover() {
        return issuerApprover;
    }

    public Party getBuyer() {
        return buyer;
    }
}
