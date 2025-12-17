package com.abhishek.limitedcart.order.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service

@Service
class StockReservationService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val reserveStockScript: RedisScript<Long>
) {

    /**
     * Atomically check and decrement stock in Redis using Lua script.
     * Returns true if reservation succeeded, false if out of stock.
     * 
     * This is the critical gatekeeper operation that prevents overselling.
     */
    fun reserveStock(productId: String, quantity: Int): Boolean {
        val key = "stock:$productId"
        val result: Long? = redisTemplate.execute(
            reserveStockScript,
            listOf(key),
            quantity.toString()
        )
        
        // Lua script returns 1 for success, 0 for failure
        return result != null && result > 0
    }

    /**
     * Get current stock from Redis without reserving.
     * Used for display purposes only - not authoritative for reservation.
     */
    fun getCurrentStock(productId: String): Int? {
        val key = "stock:$productId"
        return redisTemplate.opsForValue().get(key)?.toIntOrNull()
    }

    /**
     * Check if stock exists in Redis for a product.
     * Returns false if product not synced yet.
     */
    fun stockExists(productId: String): Boolean {
        val key = "stock:$productId"
        return redisTemplate.hasKey(key)
    }
}
