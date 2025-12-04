package com.abhishek.limitedcart.common.events

import java.math.BigDecimal
import java.time.Instant

data class OrderCreatedEvent(
    val orderId: String,
    val userId: String,
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal,
    val status: String,
    val occurredAt: Instant
)

data class OrderConfirmedEvent(
    val orderId: String,
    val userId: String,
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal,
    val status: String,
    val occurredAt: Instant
)

data class OrderFailedEvent(
    val orderId: String,
    val userId: String,
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal,
    val status: String,
    val occurredAt: Instant
)
