package com.abhishek.limitedcart.order.service

import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import com.abhishek.limitedcart.common.workflow.OrderSagaRequest
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.order.dto.CreateOrderRequest
import com.abhishek.limitedcart.order.dto.OrderResponse
import com.abhishek.limitedcart.order.entity.OrderEntity
import com.abhishek.limitedcart.order.entity.OrderStatus
import com.abhishek.limitedcart.order.messaging.OrderEventPublisher
import com.abhishek.limitedcart.order.repository.OrderRepository
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workflowClient: WorkflowClient,
    private val orderEventPublisher: OrderEventPublisher,
    @Value("\${app.temporal.orderSagaQueue}") private val taskQueue: String
) {

    @Transactional
    fun createOrder(request: CreateOrderRequest, userId: String): OrderResponse {
        val amount = calculateAmount(request.quantity)
        val order = orderRepository.save(
            OrderEntity(
                userId = userId,
                productId = request.productId,
                quantity = request.quantity,
                amount = amount,
                status = OrderStatus.PENDING
            )
        )
        orderEventPublisher.publishOrderCreated(order)
        startWorkflow(order, userId)
        return order.toResponse()
    }

    @Transactional
    fun confirmOrder(orderId: UUID, paymentId: String): OrderResponse {
        val order = loadOrder(orderId)
        order.status = OrderStatus.CONFIRMED
        order.paymentId = paymentId
        order.failureReason = null
        orderEventPublisher.publishOrderConfirmed(order)
        return order.toResponse()
    }

    @Transactional
    fun failOrder(orderId: UUID, reason: String): OrderResponse {
        val order = loadOrder(orderId)
        order.status = OrderStatus.FAILED
        order.failureReason = reason
        orderEventPublisher.publishOrderFailed(order)
        return order.toResponse()
    }

    private fun startWorkflow(order: OrderEntity, userId: String) {
        val orderId = requireNotNull(order.id).toString()
        val options = WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setWorkflowId("order-$orderId")
            .build()

        val workflow = workflowClient.newWorkflowStub(OrderWorkflow::class.java, options)
        val sagaRequest = OrderSagaRequest(
            orderId = orderId,
            userId = userId,
            productId = order.productId,
            quantity = order.quantity,
            amount = order.amount
        )
        WorkflowClient.start(workflow::processOrder, sagaRequest)
    }

    private fun loadOrder(orderId: UUID): OrderEntity =
        orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order $orderId not found") }

    private fun OrderEntity.toResponse(): OrderResponse =
        OrderResponse(id = requireNotNull(id), status = status, amount = amount)

    private fun calculateAmount(quantity: Int): BigDecimal {
        val unitPrice = BigDecimal("19.99")
        return unitPrice.multiply(BigDecimal(quantity))
    }
}
