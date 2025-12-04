package com.abhishek.limitedcart.inventory.controller

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
    private val inventoryService: InventoryService
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
}
