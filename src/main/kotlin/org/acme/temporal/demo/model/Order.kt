package org.acme.temporal.demo.model

import java.util.*

data class Order(
    var orderId: String = UUID.randomUUID().toString(),
    var goodsId: String = UUID.randomUUID().toString(),
    var goodsPrice: Int = 30,
    var goods: Goods = Goods(),
    var orderDate: Date = Date(),
    var isCheck: Boolean = false,
    var checkEmpl: String? = null,
    var checkDate: Date? = null
)
