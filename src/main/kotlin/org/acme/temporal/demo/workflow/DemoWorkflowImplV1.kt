package org.acme.temporal.demo.workflow

import io.temporal.workflow.Saga
import io.temporal.workflow.Workflow
import org.acme.temporal.demo.model.Order
import org.acme.temporal.demo.model.OrderStatus
import org.acme.temporal.demo.utils.ActivityStubUtils
import org.slf4j.LoggerFactory
import java.util.*

class DemoWorkflowImplV1 : DemoWorkflow {
    private val logger = LoggerFactory.getLogger(DemoWorkflowImplV1::class.java)

    private val demoActivityExecutor = ActivityStubUtils.getActivitiesStubWithTimeoutAndRetries(10, 3, 5)
    private val saga = Saga(Saga.Options.Builder().setParallelCompensation(false).build())

    private var status = ""
    private var processingOrder = Order()

    private var isException = false

    override fun processOrder(order: Order): Order {
        processingOrder = order
        processingOrder.status = OrderStatus.Processing

        saga.addCompensation {
            status = "Compensating Processing for the order: $processingOrder"
            this.processingOrder = demoActivityExecutor.compensateProcessing(this.processingOrder)
        }

        try {
            logger.info("Start processing the order $order...")

            // 1. pick the goods in the order
            status = "Picking the goods in the order: $processingOrder"
            processingOrder.goods = demoActivityExecutor.pickGoods(processingOrder.goodsId)

//            // 2. check the order if is valid
//            status = "Checking the order: $processingOrder"
//            logger.info("Start checking the order $processingOrder...")
//            Workflow.await { processingOrder.isCheck }
//            logger.info("Checked order by ${processingOrder.checkEmpl} at ${processingOrder.checkDate}")

            // 3. ship the order to customer
            status = "Shipping the order to customer: $processingOrder"
            processingOrder.shipDate = demoActivityExecutor.shipOrder(processingOrder, isException)
            processingOrder.status = OrderStatus.Shipped

            logger.info("Processed order: $order")
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

    override fun simulateException(isException: Boolean) {
        this.isException = isException
        logger.info("Set isException=$isException for simulating exception")
    }
}