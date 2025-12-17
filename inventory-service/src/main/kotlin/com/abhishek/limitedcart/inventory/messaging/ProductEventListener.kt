package com.abhishek.limitedcart.inventory.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.ProductCreatedEvent
import com.abhishek.limitedcart.inventory.service.InventoryService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProductEventListener(
    private val inventoryService: InventoryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.PRODUCT_CREATED], groupId = "inventory-service")
    fun onProductCreated(event: ProductCreatedEvent) {
        log.info("Received ProductCreatedEvent for productId: {}", event.productId)
        try {
            inventoryService.initializeStock(event.productId, event.name)
            log.info("Successfully initialized stock for productId: {}", event.productId)
        } catch (e: Exception) {
            log.error("Failed to initialize stock for productId: {}", event.productId, e)
        }
    }
}
