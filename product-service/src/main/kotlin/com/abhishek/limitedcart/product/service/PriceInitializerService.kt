package com.abhishek.limitedcart.product.service

import com.abhishek.limitedcart.product.repository.ProductRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service to initialize Redis with product prices on startup.
 * Syncs all active products from database to Redis cache.
 */
@Service
class PriceInitializerService(
    private val productRepository: ProductRepository,
    private val priceRedisService: PriceRedisService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initializePrices() {
        logger.info("Initializing Redis with product prices...")
        
        val activeProducts = productRepository.findAll()
            .filter { it.active }
        
        val priceMap = activeProducts.associate { 
            it.id.toString() to it.price 
        }
        
        priceRedisService.bulkSetPrices(priceMap)
        
        logger.info("Successfully initialized ${priceMap.size} product prices in Redis")
    }
}
