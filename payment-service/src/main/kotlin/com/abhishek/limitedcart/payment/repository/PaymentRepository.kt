package com.abhishek.limitedcart.payment.repository

import com.abhishek.limitedcart.payment.entity.PaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PaymentRepository : JpaRepository<PaymentEntity, UUID> {
    fun findByPaymentLinkId(paymentLinkId: String): Optional<PaymentEntity>
    fun findByOrderId(orderId: UUID): Optional<PaymentEntity>
    fun findFirstByOrderIdOrderByCreatedAtDesc(orderId: UUID): Optional<PaymentEntity>
}
