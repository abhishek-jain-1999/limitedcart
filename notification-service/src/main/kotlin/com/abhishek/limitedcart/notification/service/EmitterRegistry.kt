package com.abhishek.limitedcart.notification.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class EmitterRegistry {
    private val log = LoggerFactory.getLogger(javaClass)
    // Map userId -> List of Emitters (to support multiple tabs/devices)
    private val emitters = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()

    fun add(userId: String, emitter: SseEmitter) {
        emitters.computeIfAbsent(userId) { CopyOnWriteArrayList() }.add(emitter)
        log.info("Added emitter for user {}, total emitters: {}", userId, emitters[userId]?.size)
    }

    fun remove(userId: String, emitter: SseEmitter) {
        emitters[userId]?.let { list ->
            list.remove(emitter)
            log.info("Removed emitter for user {}, remaining: {}", userId, list.size)
            if (list.isEmpty()) {
                emitters.remove(userId)
            }
        }
    }

    fun getEmitters(userId: String): List<SseEmitter> {
        return emitters[userId] ?: emptyList()
    }
}
