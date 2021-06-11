package org.acme.temporal.demo.workflow

import org.acme.temporal.demo.model.Goods
import org.acme.temporal.demo.model.Order

class DemoActivityExecutorImpl : DemoActivityExecutor {
    override fun pickGoods(goodsId: String): Goods {
        TODO("Not yet implemented")
    }

    override fun checkOrder(order: Order): Order {
        TODO("Not yet implemented")
    }

    override fun shipOrder(order: Order) {
        TODO("Not yet implemented")
    }

    override fun compensateProcessing(order: Order): Order {
        TODO("Not yet implemented")
    }
}