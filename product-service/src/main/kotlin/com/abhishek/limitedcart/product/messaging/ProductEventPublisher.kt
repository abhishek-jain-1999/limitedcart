package com.abhishek.limitedcart.product.messaging

import com.abhishek.limitedcart.common.events.ProductCreatedEvent
import com.abhishek.limitedcart.common.events.ProductUpdatedEvent
import com.abhishek.limitedcart.product.entity.ProductEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProductEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${app.kafka.topics.productCreated}") private val productCreatedTopic: String,
    @Value("\${app.kafka.topics.productUpdated}") private val productUpdatedTopic: String
) {

    fun publishProductCreated(product: ProductEntity) {
        val event = ProductCreatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(productCreatedTopic, event.productId, event)
    }

    fun publishProductUpdated(product: ProductEntity) {
        val event = ProductUpdatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(productUpdatedTopic, event.productId, event)
    }
}
