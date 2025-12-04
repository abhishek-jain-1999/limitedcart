package com.abhishek.limitedcart.common.workflow

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.math.BigDecimal

data class OrderSagaRequest(
    val orderId: String,
    val userId: String,
    val productId: String,
    val quantity: Int,
    val amount: BigDecimal
)

@WorkflowInterface
interface OrderWorkflow {
    @WorkflowMethod
    fun processOrder(request: OrderSagaRequest)
}
