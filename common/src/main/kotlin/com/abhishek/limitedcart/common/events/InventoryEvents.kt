package com.abhishek.limitedcart.common.events

import java.time.Instant

data class InventoryUpdatedEvent(
    val productId: String,
    val availableQuantity: Int,
    val occurredAt: Instant
)
