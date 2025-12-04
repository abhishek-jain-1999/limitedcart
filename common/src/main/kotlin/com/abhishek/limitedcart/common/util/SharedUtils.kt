package com.abhishek.limitedcart.common.util

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

object SharedUtils {
    const val CORRELATION_ID_HEADER = "X-Correlation-Id"

    fun generateCorrelationId(): String = UUID.randomUUID().toString()

    fun getOrCreateCorrelationId(): String {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val existing = attributes?.request?.getHeader(CORRELATION_ID_HEADER)
        return existing?.takeIf { it.isNotBlank() } ?: generateCorrelationId()
    }
}
