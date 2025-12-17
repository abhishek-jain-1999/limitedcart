package com.abhishek.limitedcart.order.service

import com.abhishek.limitedcart.common.constants.TaskQueues
import com.abhishek.limitedcart.common.events.OrderReservationEvent
import com.abhishek.limitedcart.common.exception.OutOfStockException
import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import com.abhishek.limitedcart.common.workflow.OrderSagaRequest
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.order.dto.CreateOrderRequest
import com.abhishek.limitedcart.order.dto.OrderReservationResponse
import com.abhishek.limitedcart.order.dto.OrderResponse
import com.abhishek.limitedcart.order.entity.OrderEntity
import com.abhishek.limitedcart.order.entity.OrderStatus
import com.abhishek.limitedcart.order.messaging.OrderEventPublisher
import com.abhishek.limitedcart.order.messaging.OrderReservationPublisher
import com.abhishek.limitedcart.order.repository.OrderRepository
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workflowClient: WorkflowClient,
    private val orderEventPublisher: OrderEventPublisher,
    private val priceRedisService: PriceRedisService,
    private val stockReservationService: StockReservationService,
    private val orderReservationPublisher: OrderReservationPublisher
) {

    /**
     * GATEKEEPER PATTERN (Option 1): Hybrid approach
     * 1. Get price from Redis (fail fast)
     * 2. Reserve stock atomically in Redis
     * 3. Save minimal order to DB immediately (for user tracking)
     * 4. Publish to Kafka for async workflow processing
     * 5. Return 202 Accepted with DB-generated orderId
     * 
     * Trade-off: Small DB write penalty (~15ms) but enables immediate order tracking.
     */
    @Transactional
    fun createOrder(request: CreateOrderRequest, userId: String): OrderReservationResponse {
        // Step 1: Get price from Redis (fail fast if not synced)
        val price = priceRedisService.getPrice(request.productId)
            ?: throw IllegalStateException("Price not available for product ${request.productId}. Please try again later.")

        // Step 2: Execute atomic stock reservation using Lua script
        val reservationSuccess = stockReservationService.reserveStock(request.productId, request.quantity)
        
        if (!reservationSuccess) {
            throw OutOfStockException("Product ${request.productId} is out of stock")
        }

        // Step 3: Calculate total amount
        val totalAmount = price.multiply(request.quantity.toBigDecimal())

        // Step 4: Save minimal order to DB (for immediate user tracking)
        val order = orderRepository.save(
            OrderEntity(
                userId = userId,
                productId = request.productId,
                quantity = request.quantity,
                amount = totalAmount,
                status = OrderStatus.PENDING
            )
        )
        
        val orderId = requireNotNull(order.id).toString()

        // Step 5: Publish OrderReservationEvent to Kafka for async processing
        val event = OrderReservationEvent(
            orderId = orderId,
            userId = userId,
            productId = request.productId,
            quantity = request.quantity,
            price = price,
            totalAmount = totalAmount
        )
        orderReservationPublisher.publishOrderReservation(event)

        // Step 6: Return 202 Accepted with DB-generated orderId
        return OrderReservationResponse(
            orderId = orderId,
            status = OrderStatus.PENDING,
            message = "Order reserved successfully. Processing payment..."
        )
    }

    @Transactional
    fun confirmOrder(orderId: UUID, paymentId: String): OrderResponse {
        val order = loadOrder(orderId)
        markOrderConfirmed(order, paymentId)
        return order.toResponse()
    }

    @Transactional
    fun failOrder(orderId: UUID, reason: String): OrderResponse {
        val order = loadOrder(orderId)
        markOrderFailed(order, reason)
        return order.toResponse()
    }

    @Transactional
    fun updateProgress(orderId: UUID, status: OrderStatus, message: String?) {
        val order = loadOrder(orderId)
        markOrderProgress(order, status, message)
    }

    @Transactional
    fun handlePaymentSuccess(orderId: UUID, paymentId: String) {
        val order = loadOrder(orderId)
        markOrderConfirmed(order, paymentId)
    }

    @Transactional
    fun handlePaymentFailure(orderId: UUID, reason: String) {
        val order = loadOrder(orderId)
        markOrderFailed(order, reason)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: UUID): OrderResponse =
        loadOrder(orderId).toResponse()

    @Transactional
    fun cancelOrder(orderId: UUID, userId: String) {
        val order = loadOrder(orderId)
        
        // Only allow cancellation for certain statuses
        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.INVENTORY_RESERVED, OrderStatus.PAYMENT_PENDING)) {
            throw IllegalStateException("Cannot cancel order in ${order.status} status")
        }
        
        // Cancel the Temporal workflow
        try {
            val workflowId = "order-${orderId}"
            val workflow = workflowClient.newWorkflowStub(OrderWorkflow::class.java, workflowId)
            workflow.cancel()
        } catch (e: Exception) {
            // Workflow might not exist or already completed, log but continue
            println("Warning: Could not cancel workflow for order $orderId: ${e.message}")
        }
        
        // Mark order as cancelled
        order.status = OrderStatus.CANCELLED
        order.failureReason = "Cancelled by user"
        orderRepository.save(order)
        
        // Publish cancellation event
        orderEventPublisher.publishProgress(order, "Order cancelled by user")
    }

//    private fun startWorkflow(order: OrderEntity, userId: String) {
//        val orderId = requireNotNull(order.id).toString()
//        val options = WorkflowOptions.newBuilder()
//            .setTaskQueue(TaskQueues.ORDER_SAGA_QUEUE)
//            .setWorkflowId("order-$orderId")
//            .build()
//
//        val workflow = workflowClient.newWorkflowStub(OrderWorkflow::class.java, options)
//        val sagaRequest = OrderSagaRequest(
//            orderId = orderId,
//            userId = userId,
//            productId = order.productId,
//            quantity = order.quantity,
//            amount = order.amount
//        )
//        WorkflowClient.start(workflow::processOrder, sagaRequest)
//    }

    private fun loadOrder(orderId: UUID): OrderEntity =
        orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order $orderId not found") }

    private fun OrderEntity.toResponse(): OrderResponse =
        OrderResponse(id = requireNotNull(id), status = status, amount = amount)

    private fun calculateAmount(quantity: Int): BigDecimal {
        val unitPrice = BigDecimal("19.99")
        return unitPrice.multiply(BigDecimal(quantity))
    }

    private fun markOrderConfirmed(order: OrderEntity, paymentId: String) {
        val alreadyConfirmed = order.status == OrderStatus.CONFIRMED && order.paymentId == paymentId
        if (alreadyConfirmed) {
            return
        }
        order.status = OrderStatus.CONFIRMED
        order.paymentId = paymentId
        order.failureReason = null
        orderEventPublisher.publishProgress(order, "Order confirmed")
        orderEventPublisher.publishOrderConfirmed(order)
    }

    private fun markOrderFailed(order: OrderEntity, reason: String) {
        if (order.status == OrderStatus.FAILED && order.failureReason == reason) {
            return
        }
        order.status = OrderStatus.FAILED
        order.failureReason = reason
        orderEventPublisher.publishOrderFailed(order)
        orderEventPublisher.publishProgress(order, reason)
    }
    
    private fun markOrderProgress(order: OrderEntity, status: OrderStatus, message: String?) {
        order.status = status
        orderRepository.save(order)  // Persist status change to DB
        orderEventPublisher.publishProgress(order, message)
    }
}
