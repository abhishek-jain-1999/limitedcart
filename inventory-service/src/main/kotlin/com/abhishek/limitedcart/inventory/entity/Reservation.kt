package com.abhishek.limitedcart.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "reservations",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["order_id"])
    ]
)
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id")
    var reservationId: UUID? = null,

    @Column(name = "order_id", nullable = false)
    var orderId: String,

    @Column(name = "product_id", nullable = false)
    var productId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus = ReservationStatus.RESERVED,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: OffsetDateTime? = null
)

enum class ReservationStatus {
    RESERVED,
    CONFIRMED,
    CANCELLED
}
