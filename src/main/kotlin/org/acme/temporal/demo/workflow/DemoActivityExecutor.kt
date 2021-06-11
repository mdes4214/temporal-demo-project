package org.acme.temporal.demo.workflow

import io.temporal.activity.ActivityInterface
import org.acme.temporal.demo.model.Goods
import org.acme.temporal.demo.model.Order

@ActivityInterface
interface DemoActivityExecutor {
    fun pickGoods(goodsId: String): Goods
    fun checkOrder(order: Order): Order
    fun shipOrder(order: Order)
    fun compensateProcessing(order: Order): Order
}