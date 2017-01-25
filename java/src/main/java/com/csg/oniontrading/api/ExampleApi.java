package com.csg.oniontrading.api;

import com.csg.oniontrading.contract.*;
import com.csg.oniontrading.contract.auction.AuctionPostContract;
import com.csg.oniontrading.contract.auction.AuctionPostState;
import com.csg.oniontrading.flow.*;
import com.csg.oniontrading.model.TradingOrder;
import com.csg.oniontrading.model.auction.AuctionOrder;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
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
    public Map<String, String> whoami() { return singletonMap("me", myLegalName); }

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
    @Path("{party}/create-trade-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTradeOrder(TradingOrder order, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromName(partyName);

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
    public Response approveTrade(@PathParam("tradeId")String tradeId){
        TradingState tradingState = find(tradeId);

        TradingFlowResult result = null;
        if(isRiskManagerOf(myLegalName)) {
            result = services
                    .startFlowDynamic(IssuerRiskManagerApprove.class, tradingState)
                    .getReturnValue()
                    .toBlocking()
                    .first();
        }
        else if(myLegalName.equals(tradingState.getBuyer().getName())) {
            result = services
                    .startFlowDynamic(BuyerApproveAndSendToRiskManager.class, tradingState)
                    .getReturnValue()
                    .toBlocking()
                    .first();
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

    private boolean isRiskManagerOf(String legalName) {
        if(legalName.startsWith("Node")){
            String bankId = myLegalName.substring("Node".length());
            return myLegalName.startsWith("RiskManager") && myLegalName.endsWith(bankId);
        }
        else{
            return false;
        }

    }

    private TradingState find(String tradeId) {
        for (StateAndRef<ContractState> ref : services.vaultAndUpdates().getFirst()) {
            ContractState contractState = ref.component1().getData();
            TradingState tradingState = (TradingState) contractState;
            if(tradingState.getTradingOrder().getOrderId().equalsIgnoreCase(tradeId)){
                return tradingState;
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
}