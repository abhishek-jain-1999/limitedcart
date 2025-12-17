package com.abhishek.limitedcart.common.workflow

data class OrderWorkflowResult(
    val success: Boolean,
    val orderId: String,
    val reason: String? = null
)
