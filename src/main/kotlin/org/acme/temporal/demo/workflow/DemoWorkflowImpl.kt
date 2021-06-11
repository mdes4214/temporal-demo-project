package org.acme.temporal.demo.workflow

import io.temporal.workflow.Saga
import org.acme.temporal.demo.model.Order
import org.acme.temporal.demo.utils.ActivityStubUtils
import org.jboss.logging.Logger
import java.util.*

class DemoWorkflowImpl : DemoWorkflow {
    private val logger = Logger.getLogger(DemoWorkflowImpl::class.java)

    private val demoActivityExecutor = ActivityStubUtils.getActivitiesStubWithTimeoutAndRetries(1, 3, 5)
    private val saga = Saga(Saga.Options.Builder().setParallelCompensation(false).build())

    private var status: String = ""
    private var processingOrder: Order = Order()

    override fun processOrder(order: Order): Order {
        processingOrder = order

        saga.addCompensation {
            status = "Compensating Processing for the order: $processingOrder"
            this.processingOrder = demoActivityExecutor.compensateProcessing(this.processingOrder)
        }

        try {
            // 1. pick the goods in the order
            status = "Picking the goods in the order: $processingOrder"
            processingOrder.goods = demoActivityExecutor.pickGoods(processingOrder.goodsId)

            // 2. check the order if is valid
            status = "Validating the order: $processingOrder"
            processingOrder = demoActivityExecutor.checkOrder(processingOrder)

            // 3. ship the order to customer
            status = "Shipping the order to customer: $processingOrder"
            demoActivityExecutor.shipOrder(processingOrder)

        } catch (e: Exception) {
            logger.error("Processing Order failed, ", e)
            saga.compensate()
        }
        return processingOrder
    }

    override fun getStatus(): String = status

    override fun approve(approver: String) {
        processingOrder.isCheck = true
        processingOrder.checkEmpl = approver
        processingOrder.checkDate = Date()
    }

    override fun exit() {
        TODO("Not yet implemented")
    }
}