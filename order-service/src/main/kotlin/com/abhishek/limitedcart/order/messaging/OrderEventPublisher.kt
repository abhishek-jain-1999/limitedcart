package com.abhishek.limitedcart.order.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.OrderConfirmedEvent
import com.abhishek.limitedcart.common.events.OrderCreatedEvent
import com.abhishek.limitedcart.common.events.OrderFailedEvent
import com.abhishek.limitedcart.common.events.OrderProgressEvent
import com.abhishek.limitedcart.order.entity.OrderEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun publishOrderCreated(order: OrderEntity) =
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, order.id.toString(), order.toCreatedEvent())
    
    fun publishOrderConfirmed(order: OrderEntity) =
        kafkaTemplate.send(KafkaTopics.ORDER_CONFIRMED, order.id.toString(), order.toConfirmedEvent())
    
    fun publishOrderFailed(order: OrderEntity) =
        kafkaTemplate.send(KafkaTopics.ORDER_FAILED, order.id.toString(), order.toFailedEvent())
    
    fun publishProgress(order: OrderEntity, message: String? = null) =
        kafkaTemplate.send(KafkaTopics.ORDER_PROGRESS, order.id.toString(), order.toProgressEvent(message))
    
    private fun OrderEntity.toCreatedEvent() = OrderCreatedEvent(
        orderId = requireNotNull(id).toString(),
        userId = userId,
        productId = productId,
        quantity = quantity,
        amount = amount,
        status = status.name,
        occurredAt = Instant.now()
    )
    
    private fun OrderEntity.toConfirmedEvent() = OrderConfirmedEvent(
        orderId = requireNotNull(id).toString(),
        userId = userId,
        productId = productId,
        quantity = quantity,
        amount = amount,
        status = status.name,
        occurredAt = Instant.now()
    )
    
    private fun OrderEntity.toFailedEvent() = OrderFailedEvent(
        orderId = requireNotNull(id).toString(),
        userId = userId,
        productId = productId,
        quantity = quantity,
        amount = amount,
        status = status.name,
        occurredAt = Instant.now()
    )
    
    private fun OrderEntity.toProgressEvent(message: String?) = OrderProgressEvent(
        orderId = requireNotNull(id).toString(),
        userId = userId,
        status = status.name,
        message = message,
        occurredAt = Instant.now()
    )
    
}

