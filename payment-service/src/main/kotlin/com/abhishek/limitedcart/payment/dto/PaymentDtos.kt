package com.abhishek.limitedcart.payment.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/* ---------- API DTOs ---------- */

data class InitiatePaymentRequestDto(
    @field:NotBlank
    val orderId: String,

    @field:DecimalMin(value = "0.01", inclusive = true)
    val amount: BigDecimal,

    @field:NotBlank
    val currency: String = "USD"
)

data class InitiatePaymentResponseDto(
    val paymentId: String,
    val paymentLink: String
)

data class ProcessPaymentRequestDto(
    @field:NotBlank
    val token: String,

    @field:jakarta.validation.Valid
    val cardDetails: CardDetails
)

data class ProcessPaymentResponseDto(
    val success: Boolean,
    val paymentId: String,
    val message: String
)

data class PaymentStatusResponseDto(
    val paymentId: String,
    val orderId: String,
    val status: String,
    val paymentLink: String?,
    val updatedAt: Instant
)

data class ChargeRequestDto(
    @field:NotBlank
    val orderId: String,

    @field:DecimalMin(value = "0.01", inclusive = true)
    val amount: BigDecimal,

    @field:NotBlank
    val userId: String
)

data class ChargeResponseDto(
    val paymentId: String,
    val status: String = "SUCCESS"
)

data class RefundRequestDto(
    @field:NotBlank
    val orderId: String,

    @field:NotBlank
    val paymentId: String,

    @field:NotBlank
    val reason: String
)

data class RefundResponseDto(
    val status: String
)

data class CardDetails(
    @field:NotBlank
    @field:Pattern(regexp = "\\d{12,19}", message = "Card number must contain only digits (12-19 length)")
    val cardNumber: String,

    @field:NotBlank
    @field:Pattern(regexp = "0[1-9]|1[0-2]", message = "Expiry month must be between 01 and 12")
    val expiryMonth: String,

    @field:NotBlank
    @field:Pattern(regexp = "\\d{2}", message = "Expiry year must be two digits")
    val expiryYear: String,

    @field:NotBlank
    @field:Size(min = 3, max = 4)
    val cvc: String,

    @field:NotBlank
    val cardHolderName: String
)

/* ---------- Service DTOs ---------- */

data class InitiatePaymentCommand(
    val orderId: UUID,
    val amount: BigDecimal,
    val currency: String = "USD",
    val userId: UUID
)

data class InitiatePaymentResult(
    val paymentId: UUID,
    val paymentLink: String
)

data class ProcessPaymentCommand(
    val token: String,
    val cardDetails: CardDetails
)

data class ProcessPaymentResult(
    val success: Boolean,
    val paymentId: UUID,
    val message: String
)

data class PaymentStatusView(
    val paymentId: UUID,
    val orderId: UUID,
    val status: com.abhishek.limitedcart.payment.entity.PaymentStatus,
    val paymentLink: String?,
    val updatedAt: Instant
)

data class PaymentResult(
    val success: Boolean,
    val transactionId: String? = null,
    val errorMessage: String? = null
)
