package com.abhishek.limitedcart.order.controller

import com.abhishek.limitedcart.order.dto.ConfirmOrderRequest
import com.abhishek.limitedcart.order.dto.CreateOrderRequest
import com.abhishek.limitedcart.order.dto.FailOrderRequest
import com.abhishek.limitedcart.order.dto.OrderResponse
import com.abhishek.limitedcart.order.service.OrderService
import jakarta.validation.Valid
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

    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
        @RequestHeader(name = "X-User-Id", required = false) userId: String?
    ): ResponseEntity<OrderResponse> {
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
}
