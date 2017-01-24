package com.csg.oniontrading.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.csg.oniontrading.contract.ApprovedTradingState;
import com.csg.oniontrading.contract.TradingState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.KeyPair;
import java.util.Collections;

import static kotlin.collections.CollectionsKt.single;

/**
 * This is the "Hello World" of flows!
 *
 * It is a generic flow which facilitates the workflow required for two parties; an [IssueAndSendToRiskManager] and an [Acceptor],
 * to come to an agreement about some arbitrary data (in this case, a [PurchaseOrder]) encapsulated within a [DealState].
 *
 * As this is just an example there's no way to handle any counter-proposals. The [Acceptor] always accepts the
 * proposed state assuming it satisfies the referenced [Contract]'s issuance constraints.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * NB. All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 *
 * The flows below have been heavily commented to aid your understanding. It may also be worth reading the CorDapp
 * tutorial documentation on the Corda docsite (https://docs.corda.net) which includes a sequence diagram which clearly
 * explains each stage of the flow.
 */
public class TradingFlow {

    public static final String NAME_NODE_A = "NodeA";
    public static final String NAME_NODE_B = "NodeB";
    public static final String NAME_RISK_MANAGER_A = "RiskManagerA";
    public static final String NAME_RISK_MANAGER_B = "RiskManagerB";

    public static class IssueAndSendToRiskManager extends FlowLogic<TradingFlowResult> {

        private final TradingState tradingState;
        private final Party otherParty;
        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                CONSTRUCTING_OFFER,
                APPROVAL,
                SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION,
                VERIFYING,
                SIGNING,
                NOTARY,
                RECORDING,
                SENDING_FINAL_TRANSACTION
        );

        private static final ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step(
                "Constructing proposed purchase order.");
        private static final ProgressTracker.Step APPROVAL = new ProgressTracker.Step(
                "Offer approval by risk manager.");
        private static final ProgressTracker.Step SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION = new ProgressTracker.Step(
                "Sending purchase order to seller for review, and receiving partially signed transaction from seller in return.");
        private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step NOTARY = new ProgressTracker.Step(
                "Obtaining notary signature.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");
        private static final ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step(
                "Sending fully signed transaction to seller.");

        public IssueAndSendToRiskManager(TradingState tradingState, Party otherParty) {
            this.tradingState = tradingState;
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override public TradingFlowResult call() {
            try {
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();

                // Stage 1.
                progressTracker.setCurrentStep(CONSTRUCTING_OFFER);
                // Construct a state object which encapsulates the PurchaseOrder object.
                // We add the public keys for us and the counterparty as well as a reference to the contract code.
                final TransactionState offerMessage = new TransactionState<ContractState>(tradingState, notary);

                // Stage 2.
                progressTracker.setCurrentStep(APPROVAL);
                Party riskManagerParty = getRiskManagerParty(getServiceHub().getMyInfo().getLegalIdentity());
                send(riskManagerParty, offerMessage);

                // Stage 3.
                return new TradingFlowResult.Success(String.format("Transaction passed to Risk manager."));
            } catch(Exception ex) {
                // Just catch all exception types.
                return new TradingFlowResult.Failure(ex.getMessage());
            }
        }

        public Party getRiskManagerParty(Party source) {
            if(source.getName().equalsIgnoreCase(NAME_NODE_A)){

                return getServiceHub().getNetworkMapCache().getNodeByLegalName(NAME_RISK_MANAGER_A).getLegalIdentity();
            }
            else if(source.getName().equalsIgnoreCase(NAME_NODE_B)){
                return getServiceHub().getNetworkMapCache().getNodeByLegalName(NAME_RISK_MANAGER_B).getLegalIdentity();
            }
            else {
                throw new RuntimeException("Unknown party "+ source.getName());
            }
        }
    }

    public static class RiskManagerTradingStore extends FlowLogic<TradingFlowResult> {

        private final Party issuer;

        public RiskManagerTradingStore (Party issuer) {
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

            final TransactionState<DealState> message = this.receive(TransactionState.class, issuer)
                    .unwrap(data -> (TransactionState<DealState>) data );

            TransactionBuilder utx = message.getData().generateAgreement(message.getNotary());

            final SignedTransaction stx = utx.signWith(keyPair).toSignedTransaction(false);

            getServiceHub().recordTransactions(Collections.singletonList(stx));
            return new TradingFlowResult.Success(String.format("Transaction id %s committed to ledger.", stx.getId()));
        }
    }

    public static class RiskManagerApprove extends FlowLogic<TradingFlowResult> {

//        private final Party issuer;
        private String approverName = "someApprover";
        private TradingState tradingState;

        public RiskManagerApprove(TradingState tradingState) {
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
            if(!isRisked){
                throw new RuntimeException("too high risk, don't trade");
            }

            ApprovedTradingState approvedTradingState = new ApprovedTradingState(approverName,
                    tradingState.getSeller(),
                    getServiceHub().getMyInfo().getLegalIdentity(),
                    tradingState.getBuyer(),
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

    public static class Acceptor extends FlowLogic<TradingFlowResult> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                WAIT_FOR_AND_RECEIVE_PROPOSAL,
                GENERATING_TRANSACTION,
                SIGNING,
                SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                VERIFYING_TRANSACTION,
                RECORDING
        );

        private static final ProgressTracker.Step WAIT_FOR_AND_RECEIVE_PROPOSAL = new ProgressTracker.Step(
                "Receiving proposed purchase order from buyer.");
        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on proposed purchase order.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step(
                "Sending partially signed transaction to buyer and wait for a response.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        @Suspendable
        @Override public TradingFlowResult call() {
            try {
                // Prep.
                // Obtain a reference to our key pair.
                final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

                // Stage 3.
                progressTracker.setCurrentStep(WAIT_FOR_AND_RECEIVE_PROPOSAL);
                // All messages come off the wire as UntrustworthyData. You need to 'unwrap' it. This is an appropriate
                // place to perform some validation over what you have just received.
                final SignedTransaction message = this.receive(SignedTransaction.class, otherParty)
                        .unwrap(data -> data);
//
//                StateRef stateRef = message.getTx().getInputs().get(0);
//                TransactionState<?> transactionState = getServiceHub().loadState(stateRef);
//                System.out.println(transactionState);
//                DealState data = message.getData();
//                System.out.println(data);

                getServiceHub().recordTransactions(Collections.singleton(message));
                return new TradingFlowResult.Success(String.format("Transaction %s stored to approve.", message.getId()));
            } catch (Exception ex) {
                return new TradingFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class TradingFlowResult {
        public static class Success extends TradingFlow.TradingFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends TradingFlow.TradingFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }
}
