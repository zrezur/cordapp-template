package com.csg.oniontrading.api

import com.csg.oniontrading.contract.PurchaseOrderContract
import com.csg.oniontrading.contract.PurchaseOrderState
import com.csg.oniontrading.flow.ExampleFlow.Initiator
import com.csg.oniontrading.flow.ExampleFlowResult
import com.csg.oniontrading.model.PurchaseOrder
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val NOTARY_NAME = "Controller"

// This API is accessible from /api/example. All paths specified below are relative to it.
@javax.ws.rs.Path("example")
class ExampleApi(val services: net.corda.core.messaging.CordaRPCOps) {
    val myLegalName: String = services.nodeIdentity().legalIdentity.name

    /**
     * Returns the party name of the node providing this end-point.
     */
    @javax.ws.rs.GET
    @javax.ws.rs.Path("me")
    @javax.ws.rs.Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @javax.ws.rs.GET
    @javax.ws.rs.Path("peers")
    @javax.ws.rs.Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapUpdates().first
            .map { it.legalIdentity.name }
            .filter { it != myLegalName && it != com.csg.oniontrading.api.NOTARY_NAME })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @javax.ws.rs.GET
    @javax.ws.rs.Path("purchase-orders")
    @javax.ws.rs.Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultAndUpdates().first

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @javax.ws.rs.PUT
    @javax.ws.rs.Path("{party}/create-purchase-order")
    fun createPurchaseOrder(purchaseOrder: com.csg.oniontrading.model.PurchaseOrder, @javax.ws.rs.PathParam("party") partyName: String): javax.ws.rs.core.Response {
        val otherParty = services.partyFromName(partyName)
        if (otherParty == null) {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build()
        }

        val state = com.csg.oniontrading.contract.PurchaseOrderState(
                purchaseOrder,
                services.nodeIdentity().legalIdentity,
                otherParty,
                com.csg.oniontrading.contract.PurchaseOrderContract())

        // The line below blocks and waits for the future to resolve.
        val result: com.csg.oniontrading.flow.ExampleFlowResult = services
                .startFlow(::(com.csg.oniontrading.flow.ExampleFlow.Initiator), state, otherParty)
                .returnValue
                .toBlocking()
                .first()

        when (result) {
            is ExampleFlowResult.Success ->
                return Response
                        .status(Response.Status.CREATED)
                        .entity(result.message)
                        .build()
            is ExampleFlowResult.Failure ->
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(result.message)
                        .build()
        }
    }
}