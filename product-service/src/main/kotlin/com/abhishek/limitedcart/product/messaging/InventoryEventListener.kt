package com.abhishek.limitedcart.product.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.InventoryUpdatedEvent
import com.abhishek.limitedcart.product.repository.ProductRepository
import com.abhishek.limitedcart.product.search.ProductSearchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class InventoryEventListener(
    private val productRepository: ProductRepository,
    private val productSearchService: ProductSearchService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.INVENTORY_UPDATED], groupId = "product-service-inventory-sync")
    @Transactional
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        logger.info("Received InventoryUpdatedEvent for productId: {}, quantity: {}", event.productId, event.availableQuantity)
        
        val uuid = runCatching { UUID.fromString(event.productId) }.getOrNull()
        if (uuid == null) {
            logger.warn("Invalid product id in event: {}", event.productId)
            return
        }

        val productOpt = productRepository.findById(uuid)
        if (productOpt.isPresent) {
            val product = productOpt.get()
            val newInStockStatus = event.availableQuantity > 0
            
            if (product.inStock != newInStockStatus) {
                product.inStock = newInStockStatus
                val savedProduct = productRepository.save(product)
                logger.info("Updated product {} inStock status to {}", uuid, newInStockStatus)
                
                // Update Elasticsearch index
                productSearchService.indexProduct(savedProduct)
            } else {
                // Even if status didn't change, we might want to ensure index is up to date, 
                // but strictly speaking only status change matters for search filtering usually.
                // However, let's index it to be safe and consistent.
                productSearchService.indexProduct(product)
            }
        } else {
            logger.warn("Product {} not found for inventory update", uuid)
        }
    }
}
