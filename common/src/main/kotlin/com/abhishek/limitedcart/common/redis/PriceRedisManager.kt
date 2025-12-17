package com.abhishek.limitedcart.common.redis

/**
 * Manager for product price Redis key generation.
 * Provides consistent key format across services.
 * 
 * Key format: price:{productId}
 */
object PriceRedisManager {

    /**
     * {"orderId":"87e6193c-53bf-462a-ba4e-0fcec8bafad9","userId":"97a909ba-f1ec-4c01-9b8f-0663abd917f6","status":"PAYMENT_PENDING","message":"Waiting for customer to complete payment","occurredAt":"2025-12-16T17:32:38.201705094Z"}
     *{"orderId":"87e6193c-53bf-462a-ba4e-0fcec8bafad9","userId":"97a909ba-f1ec-4c01-9b8f-0663abd917f6","status":"FAILED","message":"Activity with activityType='InitiatePayment' failed: 'Activity task failed'. scheduledEventId=23, startedEventId=24, activityId=8877fbfb-9296-34bd-9407-b7d035be371f, identity='1@691738ba0db8', retryState=RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED","occurredAt":"2025-12-16T17:32:41.738322054Z"}
     *{"orderId":"87e6193c-53bf-462a-ba4e-0fcec8bafad9","userId":"97a909ba-f1ec-4c01-9b8f-0663abd917f6","status":"INVENTORY_RESERVED","message":"Inventory reserved successfully","occurredAt":"2025-12-16T17:32:38.173114177Z"}
     *{"orderId":"aa234dcb-f163-42a2-980b-e32a09798555","userId":"97a909ba-f1ec-4c01-9b8f-0663abd917f6","status":"PAYMENT_PENDING","message":"Waiting for customer to complete payment","occurredAt":"2025-12-16T17:52:21.519323461Z"}
     *{"orderId":"aa234dcb-f163-42a2-980b-e32a09798555","userId":"97a909ba-f1ec-4c01-9b8f-0663abd917f6","status":"FAILED","message":"Activity with activityType='InitiatePayment' failed: 'Activity task failed'. scheduledEventId=23, startedEventId=24, activityId=4561af3e-16c4-3754-9723-e63dc38658e4, identity='1@e828614aef2b', retryState=RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED","occurredAt":"2025-12-16T17:52:25.026625879Z"}
     *
     * Build Redis key for product price.
     */
    fun buildKey(productId: String): String = "price:$productId"
}
