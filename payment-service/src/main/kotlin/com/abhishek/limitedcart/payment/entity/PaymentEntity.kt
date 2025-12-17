package com.abhishek.limitedcart.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments")
data class PaymentEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val orderId: UUID,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,

    @Column(nullable = false)
    val currency: String = "USD",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "payment_link_id", unique = true)
    var paymentLinkId: String? = null,

    @Column(name = "transaction_id")
    var transactionId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
