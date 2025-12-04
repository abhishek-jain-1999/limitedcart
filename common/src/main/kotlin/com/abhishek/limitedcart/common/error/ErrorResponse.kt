package com.abhishek.limitedcart.common.error

import java.time.OffsetDateTime

/**
 * Canonical error body shared across services for consistent API responses.
 */
data class ErrorResponse(
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val correlationId: String?,
    val path: String?
)
