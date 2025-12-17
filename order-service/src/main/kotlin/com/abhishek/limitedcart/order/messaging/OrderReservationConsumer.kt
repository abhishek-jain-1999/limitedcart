package com.abhishek.limitedcart.order.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.constants.TaskQueues
import com.abhishek.limitedcart.common.events.OrderReservationEvent
import com.abhishek.limitedcart.common.workflow.OrderSagaRequest
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.order.entity.OrderEntity
import com.abhishek.limitedcart.order.entity.OrderStatus
import com.abhishek.limitedcart.order.repository.OrderRepository
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class OrderReservationConsumer(
    private val orderRepository: OrderRepository,
    private val workflowClient: WorkflowClient,
    private val orderEventPublisher: OrderEventPublisher
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_RESERVATIONS],
        groupId = "order-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun processOrderReservation(event: OrderReservationEvent, ack: Acknowledgment) {
        try {
            logger.info("Processing order reservation: orderId={}, productId={}, quantity={}", 
                event.orderId, event.productId, event.quantity)

            // Idempotency check: Order should already exist (created by gatekeeper)
            val existingOrder = orderRepository.findById(UUID.fromString(event.orderId))
            if (!existingOrder.isPresent) {
                logger.error("Order not found in DB: orderId={}. This should not happen!", event.orderId)
                ack.acknowledge()  // Ack to avoid infinite retry
                return
            }

            val order = existingOrder.get()
            
            // Additional idempotency: Check if workflow already started
            if (order.status != OrderStatus.PENDING) {
                logger.warn("Order {} already processed with status {}. Skipping.", event.orderId, order.status)
                ack.acknowledge()
                return
            }

            // Publish events for monitoring/tracking
            orderEventPublisher.publishOrderCreated(order)
            orderEventPublisher.publishProgress(order, "Order confirmed. Starting payment processing...")

            // Start Temporal workflow for payment processing
            startWorkflow(order, event.userId)

            // Acknowledge Kafka message after successful processing
            ack.acknowledge()
            logger.info("Order reservation processed successfully: orderId={}", event.orderId)

        } catch (e: Exception) {
            logger.error("Failed to process order reservation: orderId={}", event.orderId, e)
            // Don't acknowledge - message will be redelivered
            throw e
        }
    }

    private fun startWorkflow(order: OrderEntity, userId: String) {
        val orderId = requireNotNull(order.id).toString()
        val options = WorkflowOptions.newBuilder()
            .setTaskQueue(TaskQueues.ORDER_SAGA_QUEUE)
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
        logger.info("Temporal workflow started: orderId={}, workflowId=order-{}", orderId, orderId)
    }
}
