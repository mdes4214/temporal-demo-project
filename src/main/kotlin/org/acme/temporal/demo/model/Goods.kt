package org.acme.temporal.demo.model

import java.util.*

data class Goods(
    val goodsName: String = "Toy No.23",
    val goodsPrice: Int = 30,
    val goodsId: String = UUID.randomUUID().toString()
)
