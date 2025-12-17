package com.abhishek.limitedcart.common.events

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderReservationEvent(
    val orderId: String,
    val userId: String,
    val productId: String,
    val quantity: Int,
    val price: BigDecimal,
    val totalAmount: BigDecimal,
    val reservedAt: Instant = Instant.now()
)
