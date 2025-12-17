package com.abhishek.limitedcart.inventory.service

import com.abhishek.limitedcart.inventory.entity.Stock
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service

@Service
class RedisStockService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val incrementScript: RedisScript<Long> by lazy {
        val scriptText = """
            local stockKey = KEYS[1]
            local quantity = tonumber(ARGV[1])
            local currentStock = tonumber(redis.call('GET', stockKey) or "0")
            local newStock = currentStock + quantity
            redis.call('SET', stockKey, newStock)
            return newStock
        """.trimIndent()
        RedisScript.of(scriptText, Long::class.java)
    }

    /**
     * Atomically increment stock in Redis using Lua script.
     * This ensures no race conditions between read and write.
     * Returns the new stock value after increment.
     */
    fun incrementStock(productId: String, quantity: Int): Long {
        val key = "stock:$productId"
        return redisTemplate.execute(incrementScript, listOf(key), quantity.toString()) ?: 0L
    }

    /**
     * Set initial stock value ONLY if it doesn't exist.
     * Uses SETNX (Set if Not Exists) to prevent overwriting live Redis data
     * with stale DB data during service restarts.
     */
    fun syncStockToRedis(productId: String, quantity: Int) {
        val key = "stock:$productId"
        // setIfAbsent = SETNX. Returns true if set, false if key already existed.
        redisTemplate.opsForValue().setIfAbsent(key, quantity.toString())
    }

    /**
     * Get current stock from Redis.
     * Returns null if key doesn't exist.
     */
    fun getStock(productId: String): Int? {
        val key = "stock:$productId"
        return redisTemplate.opsForValue().get(key)?.toIntOrNull()
    }

    /**
     * Bulk sync all stock from database to Redis.
     * Used during startup or manual reconciliation.
     */
    fun syncAllFromDatabase(stocks: List<Stock>) {
        stocks.forEach { stock ->
            syncStockToRedis(stock.productId, stock.availableQuantity)
        }
    }

    /**
     * Clear all stock keys from Redis.
     * DANGER: Only use for testing or manual intervention.
     */
    fun clearAllStock() {
        val keys = redisTemplate.keys("stock:*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }
}
