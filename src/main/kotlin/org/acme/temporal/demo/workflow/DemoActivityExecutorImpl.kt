package org.acme.temporal.demo.workflow

import io.temporal.workflow.Workflow
import org.acme.temporal.demo.model.Goods
import org.acme.temporal.demo.model.Order
import org.acme.temporal.demo.model.OrderStatus
import org.apache.maven.shared.utils.StringUtils
import org.slf4j.LoggerFactory
import java.util.*

class DemoActivityExecutorImpl : DemoActivityExecutor {
    private val logger = LoggerFactory.getLogger(DemoActivityExecutorImpl::class.java)

    override fun pickGoods(goodsId: String): Goods {
        // simulate picking the goods by goodsId...
        logger.info("Start picking the goods by goodsId $goodsId...")
        while (true) {

        }
        sleep(30)
        val goods = Goods(goodsId = goodsId)
        logger.info("Picked goods: $goods")
        return goods
    }

    override fun shipOrder(order: Order, isException: Boolean): Date {
        // simulate shipping the order
        logger.info("Start shipping the order $order...")

        if (isException) {
            // simulate exception happened
            logger.error("Some errors happened during order shipping!!")
            throw Exception("Simulate exception happened during order shipping")
        }

        sleep(5)
        val shipDate = Date()
        logger.info("Shipped the order at $shipDate")
        return shipDate
    }

    override fun compensateProcessing(order: Order): Order {
        order.status = OrderStatus.Rejected
        // simulate rollbacking some works
        logger.info("Start rollbacking and set order status to ${order.status}")
        sleep(5)
        logger.info("Rollbacked all works about the order $order")
        return order
    }

    override fun validApprover(order: Order): Boolean {
        logger.info("Start validating approver [${order.checkEmpl}]")
        sleep(5)
        return StringUtils.equals("Andrew", order.checkEmpl)
    }

    private fun sleep(seconds: Long) = try {
        Thread.sleep(seconds * 1000)
    } catch (e: InterruptedException) {
        logger.error("Thread sleeping failed, ", e)
    }
}