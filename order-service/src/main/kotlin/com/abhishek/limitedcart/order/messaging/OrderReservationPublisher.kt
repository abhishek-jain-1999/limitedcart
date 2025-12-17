package com.abhishek.limitedcart.order.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.OrderReservationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderReservationPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Publish order reservation event to Kafka for asynchronous processing.
     * The orderId is used as the Kafka key for partitioning.
     */
    fun publishOrderReservation(event: OrderReservationEvent) {
        try {
            logger.info("Publishing order reservation: orderId={}, productId={}, quantity={}", 
                event.orderId, event.productId, event.quantity)
            
            kafkaTemplate.send(KafkaTopics.ORDER_RESERVATIONS, event.orderId, event)
                .whenComplete { result, ex ->
                    if (ex != null) {
                        logger.error("Failed to publish order reservation: orderId={}", event.orderId, ex)
                    } else {
                        logger.debug("Order reservation published successfully: orderId={}, partition={}, offset={}", 
                            event.orderId, result?.recordMetadata?.partition(), result?.recordMetadata?.offset())
                    }
                }
        } catch (e: Exception) {
            logger.error("Exception while publishing order reservation: orderId={}", event.orderId, e)
            throw e
        }
    }
}
