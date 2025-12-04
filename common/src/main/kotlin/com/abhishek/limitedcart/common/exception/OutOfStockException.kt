package com.abhishek.limitedcart.common.exception

/**
 * Shared exception to signal state when inventory cannot satisfy a request.
 */
class OutOfStockException(message: String) : RuntimeException(message)

