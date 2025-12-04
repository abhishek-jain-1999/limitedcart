package com.abhishek.limitedcart.inventory.messaging

import com.abhishek.limitedcart.common.events.InventoryUpdatedEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InventoryEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${app.kafka.topics.inventoryUpdated}") private val topicName: String
) {
    fun publishInventoryUpdated(productId: String, availableQuantity: Int) {
        val event = InventoryUpdatedEvent(
            productId = productId,
            availableQuantity = availableQuantity,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(topicName, productId, event)
    }
}
