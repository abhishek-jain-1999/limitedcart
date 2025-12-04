package com.abhishek.limitedcart.payment.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/payments")
class PaymentController {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ChargeRequest(val orderId: String, val amount: BigDecimal, val userId: String)
    data class ChargeResponse(val paymentId: String, val status: String = "SUCCESS")
    data class RefundRequest(val orderId: String, val paymentId: String, val reason: String)
    data class RefundResponse(val status: String)

    @PostMapping("/charge")
    fun charge(@RequestBody request: ChargeRequest): ResponseEntity<ChargeResponse> {
        log.info("Processing charge for order {} with amount {}", request.orderId, request.amount)
        simulateLatency()

        if (Math.random() < 0.2) {
            log.warn("Payment declined for order {}", request.orderId)
            throw ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment Declined")
        }

        val response = ChargeResponse(paymentId = UUID.randomUUID().toString())
        log.info("Payment successful for order {} -> paymentId {}", request.orderId, response.paymentId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refund")
    fun refund(@RequestBody request: RefundRequest): ResponseEntity<RefundResponse> {
        log.info("Refunding payment {} for order {} due to {}", request.paymentId, request.orderId, request.reason)
        simulateLatency()
        return ResponseEntity.ok(RefundResponse(status = "REFUNDED"))
    }

    private fun simulateLatency() {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
