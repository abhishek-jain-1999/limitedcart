package com.abhishek.limitedcart.common.events

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
    val eventType: ProductEventType,
    val active: Boolean = true,
    val occurredAt: Instant = Instant.now()
)

enum class ProductEventType {
    CREATED,
    PRICE_UPDATED,
    DEACTIVATED
}
