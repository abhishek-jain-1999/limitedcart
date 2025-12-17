package com.abhishek.limitedcart.inventory.controller

import com.abhishek.limitedcart.common.security.UserRole
import com.abhishek.limitedcart.inventory.dto.ConfirmReservationRequest
import com.abhishek.limitedcart.inventory.dto.ReleaseReservationRequest
import com.abhishek.limitedcart.inventory.dto.ReserveRequest
import com.abhishek.limitedcart.inventory.dto.ReserveResponse
import com.abhishek.limitedcart.inventory.dto.RestockRequest
import com.abhishek.limitedcart.inventory.dto.RestockResponse
import com.abhishek.limitedcart.inventory.service.InventoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
    private val reconciliationService: com.abhishek.limitedcart.inventory.service.ReconciliationService
) {

    @PostMapping("/restock")
    fun restock(@Valid @RequestBody request: RestockRequest): ResponseEntity<RestockResponse> {
        val response = inventoryService.restock(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/reserve")
    fun reserve(@Valid @RequestBody request: ReserveRequest): ResponseEntity<ReserveResponse> {
        val response = inventoryService.reserve(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/confirm")
    fun confirm(@Valid @RequestBody request: ConfirmReservationRequest): ResponseEntity<ReserveResponse> =
        ResponseEntity.ok(inventoryService.confirm(request))

    @PostMapping("/release")
    fun release(@Valid @RequestBody request: ReleaseReservationRequest): ResponseEntity<ReserveResponse> =
        ResponseEntity.ok(inventoryService.release(request))

    @org.springframework.web.bind.annotation.GetMapping("/stock")
    fun getAllStock(): ResponseEntity<List<com.abhishek.limitedcart.inventory.dto.StockView>> {
        return ResponseEntity.ok(inventoryService.getAllStock())
    }

    @org.springframework.web.bind.annotation.GetMapping("/summary")
    @org.springframework.security.access.prepost.PreAuthorize(UserRole.ADMIN_OR_WAREHOUSE)
    fun getSummary(): ResponseEntity<com.abhishek.limitedcart.inventory.dto.InventorySummary> {
        return ResponseEntity.ok(inventoryService.getSummary())
    }

    @PostMapping("/admin/sync-redis")
    @org.springframework.security.access.prepost.PreAuthorize(UserRole.HAS_ADMIN)
    fun syncRedis(): ResponseEntity<Map<String, Any>> {
        val syncedCount = inventoryService.syncAllStockToRedis()
        return ResponseEntity.ok(mapOf(
            "message" to "Redis sync completed successfully",
            "productsSynced" to syncedCount
        ))
    }

    @org.springframework.web.bind.annotation.GetMapping("/admin/reconcile")
    @org.springframework.security.access.prepost.PreAuthorize(UserRole.HAS_ADMIN)
    fun reconcileStock(): ResponseEntity<List<com.abhishek.limitedcart.inventory.service.ReconciliationService.ReconciliationReport>> {
        val reports = reconciliationService.reconcileAll()
        return ResponseEntity.ok(reports)
    }

    @org.springframework.web.bind.annotation.GetMapping("/admin/reconcile/{productId}")
    @org.springframework.security.access.prepost.PreAuthorize(UserRole.HAS_ADMIN)
    fun reconcileProduct(@org.springframework.web.bind.annotation.PathVariable productId: String): ResponseEntity<com.abhishek.limitedcart.inventory.service.ReconciliationService.ReconciliationReport> {
        val report = reconciliationService.reconcileProduct(productId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(report)
    }
}
