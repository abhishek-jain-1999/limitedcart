package com.abhishek.limitedcart.worker.activities

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import java.math.BigDecimal

@ActivityInterface
interface PaymentActivities {
    @ActivityMethod
    fun initiatePayment(orderId: String, amount: BigDecimal, userId: String): String

    @ActivityMethod
    fun refund(orderId: String, paymentId: String)
}
