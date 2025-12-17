package com.abhishek.limitedcart.order.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.ProductEventType
import com.abhishek.limitedcart.common.events.ProductUpdatedEvent
import com.abhishek.limitedcart.order.service.PriceRedisService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProductEventConsumer(
    private val priceRedisService: PriceRedisService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.PRODUCT_EVENTS],
        groupId = "order-price-sync"
    )
    fun handleProductEvent(event: ProductUpdatedEvent) {
        try {
            logger.info("Received product event: productId={}, eventType={}, price={}", 
                event.productId, event.eventType, event.price)

            when (event.eventType) {
                ProductEventType.CREATED, ProductEventType.PRICE_UPDATED -> {
                    // Sync price to Redis
                    priceRedisService.setPrice(event.productId, event.price)
                    logger.info("Price synced to Redis: productId={}, price={}", 
                        event.productId, event.price)
                }
                ProductEventType.DEACTIVATED -> {
                    // Remove price from Redis for deactivated products
                    priceRedisService.deletePrice(event.productId)
                    logger.info("Price removed from Redis for deactivated product: productId={}", 
                        event.productId)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process product event for productId={}", event.productId, e)
            // In production, you might want to send to a dead letter queue
            throw e
        }
    }
}
