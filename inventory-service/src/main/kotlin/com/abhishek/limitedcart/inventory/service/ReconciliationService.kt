package com.abhishek.limitedcart.inventory.service

import com.abhishek.limitedcart.inventory.entity.Stock
import com.abhishek.limitedcart.inventory.repository.StockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReconciliationService(
    private val stockRepository: StockRepository,
    private val redisStockService: RedisStockService
) {

    data class ReconciliationReport(
        val productId: String,
        val productName: String?,
        val redisStock: Int,
        val dbStock: Int,
        val discrepancy: Int,
        val status: ReconciliationStatus
    )

    enum class ReconciliationStatus {
        OK,           // Redis == DB (within tolerance)
        WARNING,      // Small discrepancy (< 10)
        CRITICAL      // Large discrepancy (>= 10)
    }

    /**
     * Compare Redis stock with database stock for all products.
     * Returns a report showing discrepancies.
     */
    @Transactional(readOnly = true)
    fun reconcileAll(): List<ReconciliationReport> {
        val allStock = stockRepository.findAll()
        
        return allStock.map { stock ->
            val redisStock = redisStockService.getStock(stock.productId) ?: 0
            val dbStock = stock.availableQuantity
            val discrepancy = redisStock - dbStock
            
            val status = when {
                discrepancy == 0 -> ReconciliationStatus.OK
                Math.abs(discrepancy) < 10 -> ReconciliationStatus.WARNING
                else -> ReconciliationStatus.CRITICAL
            }
            
            ReconciliationReport(
                productId = stock.productId,
                productName = stock.productName,
                redisStock = redisStock,
                dbStock = dbStock,
                discrepancy = discrepancy,
                status = status
            )
        }
    }

    /**
     * Reconcile a single product and return the report.
     */
    @Transactional(readOnly = true)
    fun reconcileProduct(productId: String): ReconciliationReport? {
        val stock = stockRepository.findById(productId).orElse(null) ?: return null
        
        val redisStock = redisStockService.getStock(productId) ?: 0
        val dbStock = stock.availableQuantity
        val discrepancy = redisStock - dbStock
        
        val status = when {
            discrepancy == 0 -> ReconciliationStatus.OK
            Math.abs(discrepancy) < 10 -> ReconciliationStatus.WARNING
            else -> ReconciliationStatus.CRITICAL
        }
        
        return ReconciliationReport(
            productId = stock.productId,
            productName = stock.productName,
            redisStock = redisStock,
            dbStock = dbStock,
            discrepancy = discrepancy,
            status = status
        )
    }
}
