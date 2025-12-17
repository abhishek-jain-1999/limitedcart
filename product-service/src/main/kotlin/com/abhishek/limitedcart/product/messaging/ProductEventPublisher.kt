package com.abhishek.limitedcart.product.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.ProductCreatedEvent
import com.abhishek.limitedcart.common.events.ProductEventType
import com.abhishek.limitedcart.common.events.ProductUpdatedEvent
import com.abhishek.limitedcart.product.entity.ProductEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProductEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    fun publishProductCreated(product: ProductEntity) {
        // Legacy event for backwards compatibility
        val legacyEvent = ProductCreatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(KafkaTopics.PRODUCT_CREATED, legacyEvent.productId, legacyEvent)
        
        // New unified event for price sync
        val event = ProductUpdatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            eventType = ProductEventType.CREATED,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(KafkaTopics.PRODUCT_EVENTS, event.productId, event)
    }

    fun publishProductUpdated(product: ProductEntity) {
        // Legacy event for backwards compatibility
        val legacyEvent = com.abhishek.limitedcart.common.events.ProductUpdatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            eventType = ProductEventType.PRICE_UPDATED,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(KafkaTopics.PRODUCT_UPDATED, legacyEvent.productId, legacyEvent)
        
        // New unified event for price sync
        val event = ProductUpdatedEvent(
            productId = requireNotNull(product.id).toString(),
            name = product.name,
            price = product.price,
            eventType = ProductEventType.PRICE_UPDATED,
            active = product.active,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(KafkaTopics.PRODUCT_EVENTS, event.productId, event)
    }
}
