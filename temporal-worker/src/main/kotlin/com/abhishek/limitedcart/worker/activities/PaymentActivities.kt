package com.abhishek.limitedcart.worker.activities

import io.temporal.activity.ActivityInterface
import java.math.BigDecimal

@ActivityInterface
interface PaymentActivities {
    fun charge(orderId: String, amount: BigDecimal, userId: String): String
    fun refund(orderId: String, paymentId: String)
}
