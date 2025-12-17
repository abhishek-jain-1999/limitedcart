package com.abhishek.limitedcart.notification.messaging

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.OrderProgressEvent
import com.abhishek.limitedcart.notification.service.EmitterRegistry
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class OrderProgressListener(
    private val emitterRegistry: EmitterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.ORDER_PROGRESS], groupId = "notification-service")
    fun onOrderProgress(event: OrderProgressEvent) {
        log.info("Received OrderProgressEvent for order: {}, user: {}, status: {}", event.orderId, event.userId, event.status)
        
        val emitters = emitterRegistry.getEmitters(event.userId)
        if (emitters.isEmpty()) {
            log.debug("No active emitters for user {}", event.userId)
            return
        }

        val deadEmitters = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name("order-progress").data(event))
            } catch (e: Exception) {
                log.warn("Failed to send event to emitter for user {}", event.userId)
                deadEmitters.add(emitter)
            }
        }

        deadEmitters.forEach { emitterRegistry.remove(event.userId, it) }
    }
}
