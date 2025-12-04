package com.abhishek.limitedcart.inventory.dto

import com.abhishek.limitedcart.inventory.entity.ReservationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

data class ReserveRequest(
    @field:NotBlank
    val orderId: String,
    @field:NotBlank
    val productId: String,
    @field:Min(1)
    val quantity: Int
)

data class ReserveResponse(
    val reservationId: UUID,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val status: ReservationStatus,
    val expiresAt: LocalDateTime?
)

data class ConfirmReservationRequest(
    @field:NotBlank
    val orderId: String,
    @field:NotNull
    val reservationId: UUID
)

data class ReleaseReservationRequest(
    @field:NotBlank
    val orderId: String,
    @field:NotNull
    val reservationId: UUID
)
