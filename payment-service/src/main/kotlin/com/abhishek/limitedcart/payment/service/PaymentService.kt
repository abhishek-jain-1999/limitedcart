package com.abhishek.limitedcart.payment.service

import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.payment.dto.InitiatePaymentCommand
import com.abhishek.limitedcart.payment.dto.InitiatePaymentResult
import com.abhishek.limitedcart.payment.dto.PaymentStatusView
import com.abhishek.limitedcart.payment.dto.ProcessPaymentCommand
import com.abhishek.limitedcart.payment.dto.ProcessPaymentResult
import com.abhishek.limitedcart.payment.entity.PaymentEntity
import com.abhishek.limitedcart.payment.entity.PaymentStatus
import com.abhishek.limitedcart.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentProcessor: PaymentProcessor,
    orderRestClient: RestClient.Builder,
    private val workflowClient: io.temporal.client.WorkflowClient,
    @Value("\${app.payment.frontendBaseUrl}") private val frontendBaseUrl: String,
    @Value("\${app.services.orderServiceUrl}") private val orderServiceUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val orderClient = orderRestClient.baseUrl(orderServiceUrl).build()

    @Transactional
    fun initiatePayment(request: InitiatePaymentCommand): InitiatePaymentResult {
        log.info("Initiating payment for order: {}", request.orderId)

        // Check if payment already exists for this order
        val existingPayment = paymentRepository.findByOrderId(request.orderId)
        if (existingPayment.isPresent) {
            val payment = existingPayment.get()
            if (payment.status == PaymentStatus.SUCCEEDED) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Payment already succeeded for this order")
            }
            // Return existing payment link if still pending
            if (payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.REQUIRES_ACTION) {
                return InitiatePaymentResult(
                    paymentId = payment.id,
                    paymentLink = payment.paymentLinkId?.let { buildPaymentLink(it) }
                        ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Existing payment missing link")
                )
            }
        }

        val paymentLinkId = UUID.randomUUID().toString()
        val payment = PaymentEntity(
            orderId = request.orderId,
            userId = request.userId,
            amount = request.amount,
            currency = request.currency,
            status = PaymentStatus.PENDING,
            paymentLinkId = paymentLinkId
        )

        val saved = paymentRepository.save(payment)
        val paymentLink = buildPaymentLink(paymentLinkId)

        log.info("Payment initiated: paymentId={}, link={}", saved.id, paymentLink)
        return InitiatePaymentResult(
            paymentId = saved.id,
            paymentLink = paymentLink
        )
    }

    @Transactional
    fun processPayment(request: ProcessPaymentCommand): ProcessPaymentResult {
        log.info("Processing payment for token: {}", request.token)

        val payment = paymentRepository.findByPaymentLinkId(request.token)
            .orElseThrow { ResourceNotFoundException("Payment not found for token: ${request.token}") }

        // Idempotency check
        if (payment.status == PaymentStatus.SUCCEEDED) {
            log.info("Payment already succeeded: {}", payment.id)
            return ProcessPaymentResult(
                success = true,
                paymentId = payment.id,
                message = "Payment already processed"
            )
        }

        if (payment.status == PaymentStatus.FAILED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment has already failed. Please initiate a new payment.")
        }

        // Process payment
        val result = paymentProcessor.charge(payment.amount, request.cardDetails)

        payment.updatedAt = Instant.now()

        if (result.success) {
            payment.status = PaymentStatus.SUCCEEDED
            payment.transactionId = result.transactionId
            paymentRepository.save(payment)

            log.info("Payment succeeded: paymentId={}, transactionId={}", payment.id, result.transactionId)

            // Notify order-service
            notifyOrderService(payment.orderId, payment.id.toString(), success = true)

            return ProcessPaymentResult(
                success = true,
                paymentId = payment.id,
                message = "Payment successful"
            )
        } else {
            payment.status = PaymentStatus.FAILED
            paymentRepository.save(payment)

            log.warn("Payment failed: paymentId={}, error={}", payment.id, result.errorMessage)

            // Notify order-service
            notifyOrderService(payment.orderId, payment.id.toString(), success = false, reason = result.errorMessage)

            return ProcessPaymentResult(
                success = false,
                paymentId = payment.id,
                message = result.errorMessage ?: "Payment failed"
            )
        }
    }

    @Transactional(readOnly = true)
    fun findLatestPayment(orderId: UUID): PaymentStatusView {
        val payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
            .orElseThrow { ResourceNotFoundException("No payment found for order: $orderId") }

        return PaymentStatusView(
            paymentId = payment.id,
            orderId = payment.orderId,
            status = payment.status,
            paymentLink = payment.paymentLinkId?.let { buildPaymentLink(it) },
            updatedAt = payment.updatedAt
        )
    }

    @Transactional
    fun refundPayment(paymentId: UUID): Boolean {
        log.info("Refunding payment: {}", paymentId)

        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { ResourceNotFoundException("Payment not found: $paymentId") }

        if (payment.status != PaymentStatus.SUCCEEDED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only refund succeeded payments")
        }

        val result = paymentProcessor.refund(paymentId)

        if (result.success) {
            payment.status = PaymentStatus.REFUNDED
            payment.updatedAt = Instant.now()
            paymentRepository.save(payment)
            log.info("Payment refunded successfully: {}", paymentId)
            return true
        }

        log.error("Refund failed for payment: {}", paymentId)
        return false
    }

    private fun notifyOrderService(orderId: UUID, paymentId: String, success: Boolean, reason: String? = null) {
        try {
            val workflowId = "order-$orderId"
            val workflowStub = workflowClient.newWorkflowStub(OrderWorkflow::class.java, workflowId)
            
            val status = if (success) "SUCCEEDED" else "FAILED"
            // If failed, we might want to pass the reason. The signal signature is (paymentId, status).
            // We might need to adjust the signal signature to include reason, or just pass status.
            // The prompt suggested: fun notifyPaymentCompleted(paymentId: String, status: String)
            // If status is FAILED, the workflow can infer failure. But where does the reason go?
            // Maybe I should update the signal signature to include reason?
            // The prompt example: fun notifyPaymentCompleted(paymentId: String, status: String)
            // I'll stick to that for now. If failed, the workflow will handle it.
            
            workflowStub.notifyPaymentCompleted(paymentId, status)
            log.info("Signaled workflow {} with payment completion: status={}", workflowId, status)
        } catch (e: Exception) {
            log.error("Failed to signal workflow for order: {}", orderId, e)
            // Don't fail the payment operation if notification fails
        }
    }

    private fun buildPaymentLink(paymentLinkId: String) = "$frontendBaseUrl/payment?token=$paymentLinkId"
}
