package com.csg.oniontrading.api;

import com.csg.oniontrading.contract.*;
import com.csg.oniontrading.contract.auction.AuctionPostContract;
import com.csg.oniontrading.contract.auction.AuctionPostState;
import com.csg.oniontrading.flow.*;
import com.csg.oniontrading.model.TradingOrder;
import com.csg.oniontrading.model.auction.AuctionOrder;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private final CordaRPCOps services;
    private final String myLegalName;

    public ExampleApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() {
        return singletonMap("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        final String NOTARY_NAME = "Controller";
        return singletonMap(
                "peers",
                services.networkMapUpdates().getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !name.equals(NOTARY_NAME))
                        .collect(toList()));
    }

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("trading-orders")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getTradingOrders() {
        return services.vaultAndUpdates().getFirst();
    }


    @PUT
    @Path("create-trade-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTradeOrder(TradingOrder order) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromName(order.getParty());

        TradingState tradingState = new TradingState(order,
                otherParty,
                services.nodeIdentity().getLegalIdentity(),
                services.partyFromName("RiskManagerA"),
                services.partyFromName("RiskManagerB"),
                new TradingContract()
        );

        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // The line below blocks and waits for the flow to return.
        final TradingFlowResult result = services
                .startFlowDynamic(IssueAndSendToRiskManager.class, tradingState, otherParty)
                .getReturnValue()
                .toBlocking()
                .first();

        final Response.Status status;
        if (result instanceof TradingFlowResult.Success) {
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return Response
                .status(status)
                .entity(result.toString())
                .build();
    }

    @POST
    @Path("/approve-trade/{tradeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveTrade(@PathParam("tradeId") String tradeId) {
        TradingState tradingState = find(tradeId);

        TradingFlowResult result = null;

        if (isRiskManager(myLegalName) && isFromSameBank(tradingState.getSeller())) {
            result = services
                    .startFlowDynamic(IssuerRiskManagerApprove.class, tradingState)
                    .getReturnValue()
                    .toBlocking()
                    .first();
        } else if (myLegalName.equals(tradingState.getBuyer().getName())) {
            result = services
                    .startFlowDynamic(BuyerApproveAndSendToRiskManager.class, tradingState)
                    .getReturnValue()
                    .toBlocking()
                    .first();
        } else if (isRiskManager(myLegalName) && isFromSameBank(tradingState.getBuyer())) {
            result = services
                    .startFlowDynamic(BuyerRiskManagerApprove.class, tradingState)
                    .getReturnValue()
                    .toBlocking()
                    .first();
        }
        else{
            throw new RuntimeException("Something wrong");
        }

        final Response.Status status;
        if (result instanceof TradingFlowResult.Success) {
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return Response
                .status(status)
                .entity(result.toString())
                .build();
    }

    private boolean isFromSameBank(Party buyer) {
        return myLegalName.substring(myLegalName.length()-1).equals(buyer.getName().substring(buyer.getName().length()-1));
    }

    private boolean isRiskManager(String legalName) {
        return legalName.startsWith("RiskManager");
//        if (legalName.startsWith("Node")) {
//            String bankId = myLegalName.substring("Node".length());
//            return myLegalName.startsWith("RiskManager") && myLegalName.endsWith(bankId);
//        } else {
//            return false;
//        }

    }

    private TradingState find(String tradeId) {
        for (StateAndRef<ContractState> ref : services.vaultAndUpdates().getFirst()) {
            ContractState contractState = ref.component1().getData();
            if(contractState instanceof TradingState){
                TradingState tradingState = (TradingState) contractState;
                if (tradingState.getTradingOrder().getOrderId().equalsIgnoreCase(tradeId)) {
                    return tradingState;
                }
            }
        }
        throw new RuntimeException("Can not find trade " + tradeId);
    }


    @POST
    @Path("auction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postAuction(AuctionOrder order) {
        AuctionPostState postAuctionState = new AuctionPostState(order,
                services.nodeIdentity().getLegalIdentity(),
                new AuctionPostContract()
        );

        // The line below blocks and waits for the flow to return.
        final TradingFlowResult result = services
                .startFlowDynamic(StoreAuction.class, postAuctionState)
                .getReturnValue()
                .toBlocking()
                .first();

        final Response.Status status;
        if (result instanceof TradingFlowResult.Success) {
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return Response
                .status(status)
                .entity(result.toString())
                .build();
    }

    @GET
    @Path("auctions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getAuctions() {
        return services.vaultAndUpdates().getFirst().stream()
                .filter(stateAndRef -> stateAndRef.getState().getData() instanceof AuctionPostState)
                .collect(toList());
    }

    @POST
    @Path("{party}/{auctionId}")
    public Response buyAction(@PathParam("party") String party, @PathParam("auctionId") String auctionId) throws ExecutionException, InterruptedException {
        Optional<StateAndRef<ContractState>> first = getAuctions().stream().filter(stateAndRef -> ((AuctionPostState) stateAndRef.getState().getData()).getAuctionOrder().getAuctionId().equals(auctionId)).findFirst();

        if (first.isPresent()) {
            StateAndRef<ContractState> contractStateStateAndRef = first.get();
            TransactionState<ContractState> state = contractStateStateAndRef.getState();
            AuctionPostState data = (AuctionPostState) state.getData();
            TradingOrder tradingOrder = new TradingOrder();
            tradingOrder.setPair(data.getAuctionOrder().getPair());
            tradingOrder.setNominal(data.getAuctionOrder().getNominal());
            tradingOrder.setTenor(data.getAuctionOrder().getTenor());

            return createTradeOrder(tradingOrder);

        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
