package com.abhishek.limitedcart.payment.service

import com.abhishek.limitedcart.payment.dto.CardDetails
import com.abhishek.limitedcart.payment.dto.PaymentResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class MockPaymentProcessor : PaymentProcessor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun charge(amount: BigDecimal, cardDetails: CardDetails): PaymentResult {
        log.info("MockPaymentProcessor: Processing charge for amount {}", amount)
        
        // Simulate processing delay
        Thread.sleep(300)
        
        // Deterministic behavior: card numbers ending in 0 fail
        val lastDigit = cardDetails.cardNumber.takeLast(1)
        val shouldFail = lastDigit == "0"
        
        return if (shouldFail) {
            log.warn("MockPaymentProcessor: Payment declined for card ending in {}", lastDigit)
            PaymentResult(
                success = false,
                errorMessage = "Payment declined by processor"
            )
        } else {
            val transactionId = "MOCK_${UUID.randomUUID()}"
            log.info("MockPaymentProcessor: Payment successful, transactionId: {}", transactionId)
            PaymentResult(
                success = true,
                transactionId = transactionId
            )
        }
    }

    override fun refund(paymentId: UUID): PaymentResult {
        log.info("MockPaymentProcessor: Processing refund for payment {}", paymentId)
        Thread.sleep(200)
        
        val refundId = "REFUND_${UUID.randomUUID()}"
        log.info("MockPaymentProcessor: Refund successful, refundId: {}", refundId)
        
        return PaymentResult(
            success = true,
            transactionId = refundId
        )
    }
}
