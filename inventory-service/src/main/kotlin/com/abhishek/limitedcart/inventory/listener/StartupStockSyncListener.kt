package com.abhishek.limitedcart.inventory.listener

import com.abhishek.limitedcart.inventory.service.InventoryService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class StartupStockSyncListener(
    private val inventoryService: InventoryService
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        try {
            logger.info("=== Initiating stock sync to Redis on startup ===")
            val syncedCount = inventoryService.syncAllStockToRedis()
            logger.info("=== Stock sync completed: {} products synced to Redis ===", syncedCount)
        } catch (e: Exception) {
            logger.error("=== Failed to sync stock to Redis on startup ===", e)
            // Continue startup even if sync fails - admin can manually trigger sync
        }
    }
}
