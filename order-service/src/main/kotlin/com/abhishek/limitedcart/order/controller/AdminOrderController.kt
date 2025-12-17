package com.abhishek.limitedcart.order.controller

import com.abhishek.limitedcart.common.security.UserRole
import com.abhishek.limitedcart.order.service.AdminOrderService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize(UserRole.HAS_ADMIN)
class AdminOrderController(
    private val adminOrderService: AdminOrderService
) {

    @GetMapping
    fun getAllOrders(
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<AdminOrderService.AdminOrderView>> {
        return ResponseEntity.ok(adminOrderService.getAllOrders(limit))
    }

    @GetMapping("/metrics")
    fun getOrderMetrics(): ResponseEntity<AdminOrderService.AdminMetrics> {
        return ResponseEntity.ok(adminOrderService.getMetrics())
    }
}
