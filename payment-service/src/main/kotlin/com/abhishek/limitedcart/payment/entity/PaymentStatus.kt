package com.abhishek.limitedcart.payment.entity

enum class PaymentStatus {
    PENDING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    REFUNDED
}
