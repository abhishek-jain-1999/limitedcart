package com.abhishek.limitedcart.common.constants

/**
 * Centralized Kafka topic names for all services.
 * These are architectural constants that represent contracts between services.
 */
object KafkaTopics {
    // Product Events
    const val PRODUCT_CREATED = "product.created"
    const val PRODUCT_UPDATED = "product.updated"
    const val PRODUCT_EVENTS = "product.events"
    
    // Order Events
    const val ORDER_CREATED = "orders.created"
    const val ORDER_CONFIRMED = "orders.confirmed"
    const val ORDER_FAILED = "orders.failed"
    const val ORDER_PROGRESS = "order.progress"
    const val ORDER_RESERVATIONS = "orders.reservations"
    
    // Inventory Events
    const val INVENTORY_UPDATED = "inventory.updated"
}
