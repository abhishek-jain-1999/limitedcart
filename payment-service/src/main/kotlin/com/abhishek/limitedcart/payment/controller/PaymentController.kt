package com.abhishek.limitedcart.payment.controller

import com.abhishek.limitedcart.common.security.UserContext
import com.abhishek.limitedcart.payment.dto.ChargeRequestDto
import com.abhishek.limitedcart.payment.dto.ChargeResponseDto
import com.abhishek.limitedcart.payment.dto.InitiatePaymentCommand
import com.abhishek.limitedcart.payment.dto.InitiatePaymentRequestDto
import com.abhishek.limitedcart.payment.dto.InitiatePaymentResponseDto
import com.abhishek.limitedcart.payment.dto.PaymentStatusResponseDto
import com.abhishek.limitedcart.payment.dto.ProcessPaymentCommand
import com.abhishek.limitedcart.payment.dto.ProcessPaymentRequestDto
import com.abhishek.limitedcart.payment.dto.ProcessPaymentResponseDto
import com.abhishek.limitedcart.payment.dto.RefundRequestDto
import com.abhishek.limitedcart.payment.dto.RefundResponseDto
import com.abhishek.limitedcart.payment.service.PaymentService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/initiate")
    fun initiatePayment(
        @Valid @RequestBody request: InitiatePaymentRequestDto,
        authentication: Authentication
    ): ResponseEntity<InitiatePaymentResponseDto> {
        val userContext = authentication.principal as UserContext
        val userId = UUID.fromString(userContext.userId)

        val orderId = runCatching { UUID.fromString(request.orderId) }.getOrElse {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderId format")
        }

        val serviceRequest = InitiatePaymentCommand(
            orderId = orderId,
            amount = request.amount,
            currency = request.currency,
            userId = userId
        )

        val response = paymentService.initiatePayment(serviceRequest)

        return ResponseEntity.ok(
            InitiatePaymentResponseDto(
                paymentId = response.paymentId.toString(),
                paymentLink = response.paymentLink
            )
        )
    }

    @PostMapping("/process")
    fun processPayment(
        @Valid @RequestBody request: ProcessPaymentRequestDto
    ): ResponseEntity<ProcessPaymentResponseDto> {
        val serviceRequest = ProcessPaymentCommand(
            token = request.token,
            cardDetails = request.cardDetails
        )

        val response = paymentService.processPayment(serviceRequest)

        return ResponseEntity.ok(
            ProcessPaymentResponseDto(
                success = response.success,
                paymentId = response.paymentId.toString(),
                message = response.message
            )
        )
    }

    @GetMapping("/order/{orderId}")
    fun getLatestPaymentForOrder(
        @PathVariable orderId: String
    ): ResponseEntity<PaymentStatusResponseDto> {
        val parsedOrderId = runCatching { UUID.fromString(orderId) }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderId format") }

        val paymentView = paymentService.findLatestPayment(parsedOrderId)
        return ResponseEntity.ok(
            PaymentStatusResponseDto(
                paymentId = paymentView.paymentId.toString(),
                orderId = paymentView.orderId.toString(),
                status = paymentView.status.name,
                paymentLink = paymentView.paymentLink,
                updatedAt = paymentView.updatedAt
            )
        )
    }

    // Legacy endpoint for backward compatibility with temporal-worker
    @PostMapping("/charge")
    fun charge(@Valid @RequestBody request: ChargeRequestDto): ResponseEntity<ChargeResponseDto> {
        log.info("Legacy charge endpoint called for order {}", request.orderId)
        
        val orderId = UUID.fromString(request.orderId)
        val userId = UUID.fromString(request.userId)

        // Use the new flow: initiate payment
        val initiateRequest = InitiatePaymentCommand(
            orderId = orderId,
            amount = request.amount,
            currency = "USD",
            userId = userId
        )

        val initiateResponse = paymentService.initiatePayment(initiateRequest)

        // For legacy compatibility, return payment ID
        // Note: In real scenario, this would need async payment completion
        return ResponseEntity.ok(
            ChargeResponseDto(
                paymentId = initiateResponse.paymentId.toString(),
                status = "PENDING"
            )
        )
    }

    @PostMapping("/refund")
    fun refund(@Valid @RequestBody request: RefundRequestDto): ResponseEntity<RefundResponseDto> {
        log.info("Refunding payment {} for order {}", request.paymentId, request.orderId)
        
        val paymentId = UUID.fromString(request.paymentId)
        val success = paymentService.refundPayment(paymentId)

        return ResponseEntity.ok(
            RefundResponseDto(
                status = if (success) "REFUNDED" else "FAILED"
            )
        )
    }
}
