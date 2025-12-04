package com.abhishek.limitedcart.inventory.dto

import jakarta.validation.constraints.Positive

data class RestockRequest(
    val productId: String,
    
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int
)

data class RestockResponse(
    val productId: String,
    val availableQuantity: Int
)
