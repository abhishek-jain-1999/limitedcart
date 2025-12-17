package com.abhishek.limitedcart.common.events

import java.time.Instant
import java.util.UUID

data class OrderProgressEvent(
    val orderId: String,
    val userId: String,
    val status: String,
    val message: String? = null,
    val occurredAt: Instant = Instant.now()
)
