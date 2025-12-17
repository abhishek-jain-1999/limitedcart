package com.abhishek.limitedcart.worker.activities.impl

import com.abhishek.limitedcart.worker.activities.OrderActivities
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OrderActivitiesImpl(
    private val orderRestClient: RestClient
) : OrderActivities {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun confirmOrder(orderId: String, paymentId: String) {
        log.info("Confirming order {} with payment {}", orderId, paymentId)
        orderRestClient.patch()
            .uri("/orders/{id}/confirm", orderId)
            .body(ConfirmRequest(paymentId))
            .retrieve()
            .toBodilessEntity()
    }

    override fun failOrder(orderId: String, reason: String) {
        log.info("Failing order {} because {}", orderId, reason)
        orderRestClient.patch()
            .uri("/orders/{id}/fail", orderId)
            .body(FailRequest(reason))
            .retrieve()
            .toBodilessEntity()
    }

    override fun updateProgress(orderId: String, status: String, message: String) {
        log.info("Updating progress for order {}: {} - {}", orderId, status, message)
        orderRestClient.post()
            .uri("/orders/{id}/progress", orderId)
            .body(UpdateProgressRequest(status, message))
            .retrieve()
            .toBodilessEntity()
    }

    private data class ConfirmRequest(val paymentId: String)
    private data class FailRequest(val reason: String)
    private data class UpdateProgressRequest(val status: String, val message: String)
}
