package com.abhishek.limitedcart.worker.activities

import io.temporal.activity.ActivityInterface

@ActivityInterface
interface InventoryActivities {
    fun reserve(orderId: String, productId: String, quantity: Int): String
    fun release(orderId: String, reservationId: String)
}
