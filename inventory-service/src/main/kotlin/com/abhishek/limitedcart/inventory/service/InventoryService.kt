package com.abhishek.limitedcart.inventory.service

import com.abhishek.limitedcart.common.exception.OutOfStockException
import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import com.abhishek.limitedcart.inventory.dto.ConfirmReservationRequest
import com.abhishek.limitedcart.inventory.dto.ReleaseReservationRequest
import com.abhishek.limitedcart.inventory.dto.ReserveRequest
import com.abhishek.limitedcart.inventory.dto.ReserveResponse
import com.abhishek.limitedcart.inventory.dto.RestockRequest
import com.abhishek.limitedcart.inventory.dto.RestockResponse
import com.abhishek.limitedcart.inventory.entity.Reservation
import com.abhishek.limitedcart.inventory.entity.ReservationStatus
import com.abhishek.limitedcart.inventory.entity.Stock
import com.abhishek.limitedcart.inventory.messaging.InventoryEventPublisher
import com.abhishek.limitedcart.inventory.repository.ReservationRepository
import com.abhishek.limitedcart.inventory.repository.StockRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InventoryService(
    private val stockRepository: StockRepository,
    private val reservationRepository: ReservationRepository,
    private val inventoryEventPublisher: InventoryEventPublisher
) {

    @Transactional
    fun initializeStock(productId: String) {
        // Idempotent check - only create if doesn't exist
        if (!stockRepository.existsById(productId)) {
            val stock = Stock(
                productId = productId,
                availableQuantity = 0
            )
            stockRepository.save(stock)
        }
    }

    @Transactional
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    fun restock(request: RestockRequest): RestockResponse {
        val stock = stockRepository.findById(request.productId)
            .orElseThrow { ResourceNotFoundException("Stock entry for product ${request.productId} not found") }
        
        stock.availableQuantity += request.quantity
        val saved = stockRepository.saveAndFlush(stock)
        
        // Publish event to notify other services
        inventoryEventPublisher.publishInventoryUpdated(saved.productId, saved.availableQuantity)
        
        return RestockResponse(
            productId = saved.productId,
            availableQuantity = saved.availableQuantity
        )
    }

    @Transactional
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    fun reserve(request: ReserveRequest): ReserveResponse {
        val existing = reservationRepository.findByOrderId(request.orderId)
        if (existing.isPresent) {
            return existing.get().toResponse()
        }

        val stock = stockRepository.findById(request.productId)
            .orElseThrow { ResourceNotFoundException("Stock entry for product ${request.productId} not found") }

        if (stock.availableQuantity < request.quantity) {
            throw OutOfStockException("Not enough inventory for product ${request.productId}")
        }

        stock.availableQuantity -= request.quantity
        stockRepository.saveAndFlush(stock)

        val reservation = Reservation(
            orderId = request.orderId,
            productId = request.productId,
            quantity = request.quantity,
            status = ReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        return reservationRepository.save(reservation).toResponse()
    }

    @Transactional
    fun confirm(request: ConfirmReservationRequest): ReserveResponse {
        val reservation = reservationRepository.findById(request.reservationId)
            .orElseThrow { ResourceNotFoundException("Reservation ${request.reservationId} not found") }

        if (reservation.orderId != request.orderId) {
            throw IllegalArgumentException("Reservation does not belong to order ${request.orderId}")
        }

        reservation.status = ReservationStatus.CONFIRMED
        return reservationRepository.save(reservation).toResponse()
    }

    @Transactional
    fun release(request: ReleaseReservationRequest): ReserveResponse {
        val reservation = reservationRepository.findById(request.reservationId)
            .orElseThrow { ResourceNotFoundException("Reservation ${request.reservationId} not found") }

        if (reservation.orderId != request.orderId) {
            throw IllegalArgumentException("Reservation does not belong to order ${request.orderId}")
        }

        if (reservation.status != ReservationStatus.CANCELLED) {
            val stock = stockRepository.findById(reservation.productId)
                .orElseThrow { ResourceNotFoundException("Stock entry for product ${reservation.productId} not found") }
            stock.availableQuantity += reservation.quantity
            stockRepository.saveAndFlush(stock)
            reservation.status = ReservationStatus.CANCELLED
        }

        return reservationRepository.save(reservation).toResponse()
    }

    private fun Reservation.toResponse(): ReserveResponse = ReserveResponse(
        reservationId = requireNotNull(reservationId),
        orderId = orderId,
        productId = productId,
        quantity = quantity,
        status = status,
        expiresAt = expiresAt
    )
}
