package com.abhishek.limitedcart.inventory.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.InventoryUpdatedEvent
import com.abhishek.limitedcart.inventory.entity.Stock
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InventoryEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publishInventoryUpdated(productId: String, availableQuantity: Int) {
        val event = InventoryUpdatedEvent(
            productId = productId,
            availableQuantity = availableQuantity,
            occurredAt = Instant.now()
        )
        kafkaTemplate.send(KafkaTopics.INVENTORY_UPDATED, productId, event)
    }
}
