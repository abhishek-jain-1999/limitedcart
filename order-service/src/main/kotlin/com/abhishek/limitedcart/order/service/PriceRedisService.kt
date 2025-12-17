package com.abhishek.limitedcart.order.service

import com.abhishek.limitedcart.common.redis.PriceRedisManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PriceRedisService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    /**
     * Set product price in Redis.
     * Key format: price:{productId}
     */
    fun setPrice(productId: String, price: BigDecimal) {
        val key = PriceRedisManager.buildKey(productId)
        redisTemplate.opsForValue().set(key, price.toString())
    }

    /**
     * Get product price from Redis.
     * Returns null if price doesn't exist.
     */
    fun getPrice(productId: String): BigDecimal? {
        val key = PriceRedisManager.buildKey(productId)
        val priceStr = redisTemplate.opsForValue().get(key)
        return priceStr?.toBigDecimalOrNull()
    }

    /**
     * Delete product price from Redis (for deactivated products).
     */
    fun deletePrice(productId: String) {
        val key = PriceRedisManager.buildKey(productId)
        redisTemplate.delete(key)
    }
}
