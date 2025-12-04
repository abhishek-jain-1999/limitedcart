package com.abhishek.limitedcart.worker.activities.impl

import com.abhishek.limitedcart.worker.activities.PaymentActivities
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.util.UUID

@Component
class PaymentActivitiesImpl(
    private val paymentRestClient: RestClient
) : PaymentActivities {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun charge(orderId: String, amount: BigDecimal, userId: String): String {
        log.info("Charging payment for order {} amount {}", orderId, amount)
        val response = paymentRestClient.post()
            .uri("/payments/charge")
            .body(ChargeRequest(orderId, amount, userId))
            .retrieve()
            .body(ChargeResponse::class.java)
            ?: throw IllegalStateException("Charge response missing")
        return response.paymentId
    }

    override fun refund(orderId: String, paymentId: String) {
        log.info("Refunding payment {} for order {}", paymentId, orderId)
        paymentRestClient.post()
            .uri("/payments/refund")
            .body(RefundRequest(orderId, paymentId, reason = "Saga compensation"))
            .retrieve()
            .toBodilessEntity()
    }

    private data class ChargeRequest(val orderId: String, val amount: BigDecimal, val userId: String)
    private data class ChargeResponse(val paymentId: String, val status: String)
    private data class RefundRequest(val orderId: String, val paymentId: String, val reason: String)
}
