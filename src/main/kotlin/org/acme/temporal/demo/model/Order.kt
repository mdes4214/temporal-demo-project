package org.acme.temporal.demo.model

import java.util.*

data class Order(
    var orderId: String = UUID.randomUUID().toString(),
    var status: OrderStatus = OrderStatus.New,
    var goodsId: String = UUID.randomUUID().toString(),
    var goods: Goods? = null,
    var orderDate: Date = Date(),
    var isCheck: Boolean = false,
    var checkEmpl: String? = null,
    var checkDate: Date? = null,
    var shipDate: Date? = null
)
