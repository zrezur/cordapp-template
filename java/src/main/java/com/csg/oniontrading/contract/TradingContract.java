package com.csg.oniontrading.contract;

import kotlin.Unit;
import net.corda.core.Utils;
import net.corda.core.contracts.*;
import net.corda.core.contracts.clauses.AnyComposition;
import net.corda.core.contracts.clauses.Clause;
import net.corda.core.contracts.clauses.GroupClauseVerifier;
import net.corda.core.crypto.SecureHash;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static kotlin.collections.CollectionsKt.single;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TradingContract implements Contract {
    /**
     * This is a reference to the underlying legal contract template and associated parameters.
     */
    private final SecureHash legalContractReference = SecureHash.sha256("purchase order contract template and params");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }



    @Override
    public void verify(TransactionForContract transactionForContract) throws IllegalArgumentException {
        throw new NotImplementedException();
    }

    public interface Commands extends CommandData {
        class Trade implements IssueCommand, TradingContract.Commands {
            private final long nonce = Utils.random63BitValue();
            @Override public long getNonce() { return nonce; }
        }

        class Approve implements IssueCommand, TradingContract.Commands{
            private final long nonce = Utils.random63BitValue();
            @Override public long getNonce() { return nonce; }
        }
    }

    public interface Clauses {
        /**
         * Checks for the existence of a timestamp.
         */
        class Timestamp extends Clause<ContractState, TradingContract.Commands, Unit> {
            @Override public Set<TradingContract.Commands> verify(TransactionForContract tx,
                                                                        List<? extends ContractState> inputs,
                                                                        List<? extends ContractState> outputs,
                                                                        List<? extends AuthenticatedObject<? extends TradingContract.Commands>> commands,
                                                                        Unit groupingKey) {

                requireNonNull(tx.getTimestamp(), "must be timestamped");

                // We return an empty set because we don't process any commands
                return Collections.emptySet();
            }
        }

        // If you add additional clauses, make sure to reference them within the 'AnyComposition()' clause.
        class Group extends GroupClauseVerifier<PurchaseOrderState, PurchaseOrderContract.Commands, UniqueIdentifier> {
            public Group() { super(new AnyComposition<>(new PurchaseOrderContract.Clauses.Place())); }

            @Override public List<TransactionForContract.InOutGroup<PurchaseOrderState, UniqueIdentifier>> groupStates(TransactionForContract tx) {
                // Group by purchase order linearId for in/out states.
                return tx.groupStates(PurchaseOrderState.class, PurchaseOrderState::getLinearId);
            }
        }

        /**
         * Checks various requirements for the placement of a purchase order.
         */
        class Trade extends Clause<TradingState, TradingContract.Commands, UniqueIdentifier> {
            @Override public Set<TradingContract.Commands> verify(TransactionForContract tx,
                                                                        List<? extends TradingState> inputs,
                                                                        List<? extends TradingState> outputs,
                                                                        List<? extends AuthenticatedObject<? extends TradingContract.Commands>> commands,
                                                                        UniqueIdentifier groupingKey)
            {
                final AuthenticatedObject<TradingContract.Commands.Trade> command = requireSingleCommand(tx.getCommands(), TradingContract.Commands.Trade.class);
                final TradingState out = single(outputs);
                final Instant time = tx.getTimestamp().getMidpoint();

                requireThat(require -> {
//                    // Generic constraints around generation of the issue purchase order transaction.
//                    require.by("No inputs should be consumed when issuing a purchase order.",
//                            inputs.isEmpty());
//                    require.by("Only one output state should be created for each group.",
//                            outputs.size() == 1);
//                    require.by("The buyer and the seller cannot be the same entity.",
//                            out.getBuyer() != out.getSeller());
//                    require.by("All of the participants must be signers.",
//                            command.getSigners().containsAll(out.getParticipants()));
//
//                    // Purchase order specific constraints.
//                    require.by("We only deliver to the UK.",
//                            out.getPurchaseOrder().getDeliveryAddress().getCountry().equals("UK"));
//                    require.by("You must order at least one type of item.",
//                            !out.getPurchaseOrder().getItems().isEmpty());
//                    require.by("You cannot order zero or negative amounts of an item.",
//                            out.getPurchaseOrder().getItems().stream().allMatch(item -> item.getAmount() > 0));
//                    require.by("You can only order up to 100 items in total.",
//                            out.getPurchaseOrder().getItems().stream().mapToInt(PurchaseOrder.Item::getAmount).sum() <= 100);
//                    require.by("The delivery date must be in the future.",
//                            out.getPurchaseOrder().getDeliveryDate().toInstant().isAfter(time));

                    return null;
                });

                return Collections.singleton(command.getValue());
            }
        }
    }


}
