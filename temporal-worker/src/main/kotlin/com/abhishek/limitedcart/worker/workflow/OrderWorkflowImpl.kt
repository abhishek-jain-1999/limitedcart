package com.abhishek.limitedcart.worker.workflow

import com.abhishek.limitedcart.common.workflow.OrderSagaRequest
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.worker.activities.InventoryActivities
import com.abhishek.limitedcart.worker.activities.OrderActivities
import com.abhishek.limitedcart.worker.activities.PaymentActivities
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class OrderWorkflowImpl : OrderWorkflow {

    private val activityOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .setRetryOptions(
            RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .build()
        )
        .build()

    private val inventoryActivities = Workflow.newActivityStub(InventoryActivities::class.java, activityOptions)
    private val paymentActivities = Workflow.newActivityStub(PaymentActivities::class.java, activityOptions)
    private val orderActivities = Workflow.newActivityStub(OrderActivities::class.java, activityOptions)

    override fun processOrder(request: OrderSagaRequest) {
        var reservationId: String? = null
        var paymentId: String? = null
        try {
            reservationId = inventoryActivities.reserve(request.orderId, request.productId, request.quantity)
            paymentId = paymentActivities.charge(request.orderId, request.amount, request.userId)
            orderActivities.confirmOrder(request.orderId, paymentId)
        } catch (ex: Exception) {
            if (paymentId != null) {
                paymentActivities.refund(request.orderId, paymentId)
            }
            if (reservationId != null) {
                inventoryActivities.release(request.orderId, reservationId)
            }
            orderActivities.failOrder(request.orderId, ex.message ?: "Unknown error")
            throw ex
        }
    }
}
