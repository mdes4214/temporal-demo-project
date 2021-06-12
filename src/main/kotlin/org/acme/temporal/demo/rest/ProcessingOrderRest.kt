package org.acme.temporal.demo.rest

import io.temporal.client.WorkflowOptions
import org.acme.temporal.demo.WorkflowApplicationObserver
import org.acme.temporal.demo.model.Order
import org.acme.temporal.demo.workflow.DemoWorkflow
import org.slf4j.LoggerFactory
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@ApplicationScoped
@Path("/processingOrder")
@Tag(name = "Order Processing Endpoint")
class ProcessingOrderRest {
    private val logger = LoggerFactory.getLogger(ProcessingOrderRest::class.java)

    @Inject
    lateinit var observer: WorkflowApplicationObserver

    @ConfigProperty(name = "processing.order.task.queue")
    var taskQueue: String? = null

    @POST
    @Operation(summary = "Send an order to process")
    fun doProcess(@RequestBody(description = "the order data (if body is empty, will generate a default order)") inOrder: Order?): Order {
        val order = inOrder ?: Order()
        val workflow = observer.getClient().newWorkflowStub(
            DemoWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setWorkflowId(order.orderId)
                .setTaskQueue(taskQueue)
                .build()
        )
        return workflow.processOrder(order)
    }

    @GET
    @Operation(summary = "Get status of the order")
    fun getStatus(@Parameter(description = "the workflow id (equals to the order id)") @QueryParam("id") orderId: String): String {
        try {
            val workflow = observer.getClient().newWorkflowStub(DemoWorkflow::class.java, orderId)
            return workflow.getStatus()
        } catch (e: Exception) {
            logger.error("Unable to query workflow with id: $orderId, ", e)
            return "Unable to query workflow with id: $orderId"
        }
    }

    @POST
    @Path("/approve")
    @Operation(summary = "Approve (check and sign) the order")
    fun approve(
        @Parameter(description = "the workflow id (equals to the order id)") @QueryParam("id") orderId: String,
        @Parameter(description = "the approver to check order and sign") @QueryParam("approver") approver: String
    ): String {
        try {
            val workflow = observer.getClient().newWorkflowStub(DemoWorkflow::class.java, orderId)
            workflow.approve(approver)
            return workflow.getStatus()
        } catch (e: Exception) {
            logger.error("Unable to signal workflow with id: $orderId, and approver: $approver", e)
            return "Unable to signal workflow with id: $orderId, and approver: $approver"
        }
    }
}