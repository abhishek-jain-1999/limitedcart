package com.abhishek.limitedcart.common.events

import java.math.BigDecimal
import java.time.Instant

data class ProductCreatedEvent(
    val productId: String,
    val name: String,
    val price: BigDecimal,
    val active: Boolean,
    val occurredAt: Instant
)

data class ProductUpdatedEvent(
    val productId: String,
    val name: String,
    val price: BigDecimal,
    val active: Boolean,
    val occurredAt: Instant
)
