package com.abhishek.limitedcart.order.dto

import com.abhishek.limitedcart.order.entity.OrderStatus

data class OrderReservationResponse(
    val orderId: String,
    val status: OrderStatus,
    val message: String
)
