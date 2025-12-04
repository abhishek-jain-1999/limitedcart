package com.abhishek.limitedcart.common.event

import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Event<T>(
    val eventId: String = UUID.randomUUID().toString(),
    val correlationId: String? = null,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
    val type: String? = null,
    val payload: T? = null
) : Serializable
