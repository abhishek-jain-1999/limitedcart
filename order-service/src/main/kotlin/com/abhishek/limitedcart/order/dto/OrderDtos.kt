package com.abhishek.limitedcart.order.dto

import com.abhishek.limitedcart.order.entity.OrderStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class CreateOrderRequest(
    @field:NotBlank
    val productId: String,
    @field:Min(1)
    val quantity: Int
)

data class OrderResponse(
    val id: UUID,
    val status: OrderStatus,
    val amount: BigDecimal
)

data class ConfirmOrderRequest(
    @field:NotBlank
    val paymentId: String
)

data class FailOrderRequest(
    @field:NotBlank
    val reason: String
)
