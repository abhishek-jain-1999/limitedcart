package com.abhishek.limitedcart.notification.controller

import com.abhishek.limitedcart.common.security.UserContext
import com.abhishek.limitedcart.notification.service.EmitterRegistry
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val emitterRegistry: EmitterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/stream")
    fun stream(authentication: Authentication): SseEmitter {
        val userContext = authentication.principal as UserContext
        val userId = userContext.userId
        
        val emitter = SseEmitter(Long.MAX_VALUE)
        emitter.onCompletion { emitterRegistry.remove(userId, emitter) }
        emitter.onTimeout { emitterRegistry.remove(userId, emitter) }
        emitter.onError { emitterRegistry.remove(userId, emitter) }
        
        emitterRegistry.add(userId, emitter)
        log.info("New SSE stream for user {}", userId)
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connected to notification stream"))
        } catch (e: Exception) {
            emitterRegistry.remove(userId, emitter)
        }
        
        return emitter
    }
}
