package org.acme.temporal.demo.workflow

import io.temporal.activity.ActivityInterface
import org.acme.temporal.demo.model.Goods
import org.acme.temporal.demo.model.Order
import java.util.*

@ActivityInterface
interface DemoActivityExecutor {
    fun pickGoods(goodsId: String): Goods
    fun shipOrder(order: Order): Date
    fun compensateProcessing(order: Order): Order
}