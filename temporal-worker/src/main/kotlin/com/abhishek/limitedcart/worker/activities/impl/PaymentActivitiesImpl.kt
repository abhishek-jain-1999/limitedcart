package com.abhishek.limitedcart.worker.activities.impl

import com.abhishek.limitedcart.worker.activities.PaymentActivities
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@Component
class PaymentActivitiesImpl(
    private val paymentRestClient: RestClient
) : PaymentActivities {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun initiatePayment(orderId: String, amount: BigDecimal, userId: String): String {
        log.info("Initiating payment for order {} amount {}", orderId, amount)
        
        val response = paymentRestClient.post()
            .uri("/payments/initiate")
            .body(mapOf(
                "orderId" to orderId,
                "userId" to userId,
                "amount" to amount,
                "currency" to "USD"
            ))
            .retrieve()
            .body(InitiatePaymentResult::class.java)
            
        return response?.paymentId?.toString() ?: throw IllegalStateException("Failed to initiate payment")
    }

    private data class InitiatePaymentResult(val paymentId: java.util.UUID, val paymentLink: String)

    override fun refund(orderId: String, paymentId: String) {
        log.info("Refunding payment {} for order {}", paymentId, orderId)
        paymentRestClient.post()
            .uri("/payments/refund")
            .body(RefundRequest(orderId, paymentId, reason = "Saga compensation"))
            .retrieve()
            .toBodilessEntity()
    }

    private data class RefundRequest(val orderId: String, val paymentId: String, val reason: String)
    private data class PaymentStatusResponse(
        val paymentId: String,
        val orderId: String,
        val status: String
    )

    private fun fetchLatestPayment(orderId: String): PaymentStatusResponse? =
        try {
            paymentRestClient.get()
                .uri("/payments/order/{orderId}", orderId)
                .retrieve()
                .body(PaymentStatusResponse::class.java)
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
}
