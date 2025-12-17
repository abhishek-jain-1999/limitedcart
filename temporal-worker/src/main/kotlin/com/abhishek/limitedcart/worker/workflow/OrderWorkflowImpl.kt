package com.abhishek.limitedcart.worker.workflow

import com.abhishek.limitedcart.common.workflow.OrderSagaRequest
import com.abhishek.limitedcart.common.workflow.OrderWorkflow
import com.abhishek.limitedcart.common.workflow.OrderWorkflowResult
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

    private val paymentActivityOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))
        .setRetryOptions(
            RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .build()
        )
        .build()

    private val inventoryActivities = Workflow.newActivityStub(InventoryActivities::class.java, activityOptions)
    private val paymentActivities = Workflow.newActivityStub(PaymentActivities::class.java, paymentActivityOptions)
    private val orderActivities = Workflow.newActivityStub(OrderActivities::class.java, activityOptions)

    private var paymentCompleted = false
    private var paymentStatus: String? = null
    private var paymentId: String? = null
    private var cancelled = false

    override fun notifyPaymentCompleted(paymentId: String, status: String) {
        this.paymentId = paymentId
        this.paymentStatus = status
        this.paymentCompleted = true
    }

    override fun cancel() {
        this.cancelled = true
    }

    override fun processOrder(request: OrderSagaRequest): OrderWorkflowResult {
        var reservationId: String? = null
        
        try {
            // Check cancellation before reserving inventory
            if (cancelled) {
                throw RuntimeException("Order cancelled by user")
            }
            Workflow.sleep(Duration.ofSeconds(3))
            reservationId = inventoryActivities.reserve(request.orderId, request.productId, request.quantity)
            orderActivities.updateProgress(request.orderId, "INVENTORY_RESERVED", "Inventory reserved successfully")
            
            // Add a 5 second delay
            Workflow.sleep(Duration.ofSeconds(2))
            
            // Check cancellation before payment
            if (cancelled) {
                throw RuntimeException("Order cancelled by user")
            }

            orderActivities.updateProgress(request.orderId, "PAYMENT_PENDING", "Waiting for customer to complete payment")
            
            // Wait for payment signal OR cancellation
            Workflow.await { paymentCompleted || cancelled }
            
            // Check if cancelled during wait
            if (cancelled) {
                throw RuntimeException("Order cancelled by user")
            }
            
            if (paymentStatus == "SUCCEEDED") {
                orderActivities.updateProgress(request.orderId, "PAYMENT_CONFIRMED", "Payment confirmed, finalizing order")
                orderActivities.confirmOrder(request.orderId, paymentId!!)
                
                // Success!
                return OrderWorkflowResult(
                    success = true,
                    orderId = request.orderId
                )
            } else {
                throw RuntimeException("Payment failed with status: $paymentStatus")
            }
            
        } catch (ex: Exception) {
            // Compensate - refund and release inventory
            if (paymentId != null && paymentStatus == "SUCCEEDED") {
                paymentActivities.refund(request.orderId, paymentId!!)
            }
            if (reservationId != null) {
                inventoryActivities.release(request.orderId, reservationId)
            }
            orderActivities.failOrder(request.orderId, ex.message ?: "Unknown error")
            
            // Return failure result instead of throwing
            return OrderWorkflowResult(
                success = false,
                orderId = request.orderId,
                reason = ex.message ?: "Unknown error"
            )
        }
    }
}
