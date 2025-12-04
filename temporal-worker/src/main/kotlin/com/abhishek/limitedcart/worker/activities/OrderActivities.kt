package com.abhishek.limitedcart.worker.activities

import io.temporal.activity.ActivityInterface

@ActivityInterface
interface OrderActivities {
    fun confirmOrder(orderId: String, paymentId: String)
    fun failOrder(orderId: String, reason: String)
}
