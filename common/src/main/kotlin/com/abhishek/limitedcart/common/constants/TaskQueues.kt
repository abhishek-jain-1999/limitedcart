package com.abhishek.limitedcart.common.constants

/**
 * Centralized Temporal task queue names.
 * These are code contracts between workflow starters and workers.
 */
object TaskQueues {
    const val ORDER_SAGA_QUEUE = "ORDER_SAGA_QUEUE"
}
