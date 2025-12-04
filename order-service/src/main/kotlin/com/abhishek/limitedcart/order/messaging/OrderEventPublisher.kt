package com.abhishek.limitedcart.order.messaging

import com.abhishek.limitedcart.common.events.OrderConfirmedEvent
import com.abhishek.limitedcart.common.events.OrderCreatedEvent
import com.abhishek.limitedcart.common.events.OrderFailedEvent
import com.abhishek.limitedcart.order.entity.OrderEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${app.kafka.topics.orderCreated}") private val createdTopic: String,
    @Value("\${app.kafka.topics.orderConfirmed}") private val confirmedTopic: String,
    @Value("\${app.kafka.topics.orderFailed}") private val failedTopic: String
) {

    fun publishOrderCreated(order: OrderEntity) =
        kafkaTemplate.send(createdTopic, order.id.toString(), order.toCreatedEvent())

    fun publishOrderConfirmed(order: OrderEntity) =
        kafkaTemplate.send(confirmedTopic, order.id.toString(), order.toConfirmedEvent())

    fun publishOrderFailed(order: OrderEntity) =
        kafkaTemplate.send(failedTopic, order.id.toString(), order.toFailedEvent())

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
}
