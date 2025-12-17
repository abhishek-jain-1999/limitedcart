package com.abhishek.limitedcart.order.entity

import com.abhishek.limitedcart.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class OrderEntity(
    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(name = "product_id", nullable = false)
    var productId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "payment_id")
    var paymentId: String? = null,

    @Column(name = "failure_reason", columnDefinition = "text")
    var failureReason: String? = null
) : BaseEntity()

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    CANCELLED
}
