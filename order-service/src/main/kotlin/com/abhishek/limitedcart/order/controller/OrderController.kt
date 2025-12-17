package com.abhishek.limitedcart.order.controller

import com.abhishek.limitedcart.order.dto.ConfirmOrderRequest
import com.abhishek.limitedcart.order.dto.CreateOrderRequest
import com.abhishek.limitedcart.order.dto.FailOrderRequest
import com.abhishek.limitedcart.order.dto.OrderResponse
import com.abhishek.limitedcart.order.entity.OrderStatus
import com.abhishek.limitedcart.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: UUID
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.getOrder(orderId))

    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
        @RequestHeader(name = "X-User-Id", required = false) userId: String?
    ): ResponseEntity<com.abhishek.limitedcart.order.dto.OrderReservationResponse> {
        val result = orderService.createOrder(request, userId ?: "demo-user")
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result)
    }

    @PatchMapping("/{orderId}/confirm")
    fun confirmOrder(
        @PathVariable orderId: UUID,
        @RequestBody request: ConfirmOrderRequest
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.confirmOrder(orderId, request.paymentId))

    @PatchMapping("/{orderId}/fail")
    fun failOrder(
        @PathVariable orderId: UUID,
        @RequestBody request: FailOrderRequest
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.failOrder(orderId, request.reason))

    @PostMapping("/{orderId}/progress")
    fun updateProgress(
        @PathVariable orderId: UUID,
        @RequestBody request: UpdateProgressRequest
    ): ResponseEntity<Void> {
        orderService.updateProgress(orderId, request.status, request.message)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{orderId}/payment-success")
    fun handlePaymentSuccess(
        @PathVariable orderId: UUID,
        @RequestBody request: PaymentSuccessRequest
    ): ResponseEntity<Void> {
        orderService.handlePaymentSuccess(orderId, request.paymentId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{orderId}/payment-failure")
    fun handlePaymentFailure(
        @PathVariable orderId: UUID,
        @RequestBody request: PaymentFailureRequest
    ): ResponseEntity<Void> {
        orderService.handlePaymentFailure(orderId, request.reason)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: UUID,
        @RequestHeader(name = "X-User-Id", required = false) userId: String?
    ): ResponseEntity<Void> {
        orderService.cancelOrder(orderId, userId ?: "demo-user")
        return ResponseEntity.ok().build()
    }
}

data class UpdateProgressRequest(
    val status: OrderStatus,
    val message: String? = null
)

data class PaymentSuccessRequest(
    val paymentId: String
)

data class PaymentFailureRequest(
    val paymentId: String,
    val reason: String
)
