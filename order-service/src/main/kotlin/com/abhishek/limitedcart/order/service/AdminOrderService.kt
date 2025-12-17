package com.abhishek.limitedcart.order.service

import com.abhishek.limitedcart.order.repository.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class AdminOrderService(
    private val orderRepository: OrderRepository
) {
    data class AdminOrderView(
        val id: String,
        val userId: String,
        val productId: String,
        val amount: BigDecimal,
        val status: String,
        val paymentId: String?,
        val createdAt: Instant
    )

    data class AdminMetrics(
        val totalOrders: Long,
        val totalRevenue: BigDecimal,
        val confirmedOrders: Long,
        val pendingOrders: Long
    )

    @Transactional(readOnly = true)
    fun getAllOrders(limit: Int = 100): List<AdminOrderView> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        val orders = orderRepository.findAll(pageable)
        
        return orders.content.map { order ->
            AdminOrderView(
                id = requireNotNull(order.id).toString(),
                userId = order.userId,
                productId = order.productId,
                amount = order.amount,
                status = order.status.name,
                paymentId = order.paymentId,
                createdAt = order.createdAt?.atZone(ZoneOffset.UTC)?.toInstant() ?: Instant.now()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getMetrics(): AdminMetrics {
        val allOrders = orderRepository.findAll()
        
        return AdminMetrics(
            totalOrders = allOrders.size.toLong(),
            totalRevenue = allOrders
                .filter { it.status == com.abhishek.limitedcart.order.entity.OrderStatus.CONFIRMED }
                .map { it.amount }
                .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) },
            confirmedOrders = allOrders.count { it.status == com.abhishek.limitedcart.order.entity.OrderStatus.CONFIRMED }.toLong(),
            pendingOrders = allOrders.count { it.status == com.abhishek.limitedcart.order.entity.OrderStatus.PENDING }.toLong()
        )
    }
}
