package com.abhishek.limitedcart.worker.activities.impl

import com.abhishek.limitedcart.worker.activities.InventoryActivities
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class InventoryActivitiesImpl(
    private val inventoryRestClient: RestClient
) : InventoryActivities {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun reserve(orderId: String, productId: String, quantity: Int): String {
        log.info("Reserving inventory for order {} product {} quantity {}", orderId, productId, quantity)
        val response = inventoryRestClient.post()
            .uri("/inventory/reserve")
            .body(ReserveRequest(orderId, productId, quantity))
            .retrieve()
            .body(ReserveResponse::class.java)
            ?: throw IllegalStateException("Reservation response missing")
        return response.reservationId
    }

    override fun release(orderId: String, reservationId: String) {
        log.info("Releasing reservation {} for order {}", reservationId, orderId)
        inventoryRestClient.post()
            .uri("/inventory/release")
            .body(ReleaseRequest(orderId, reservationId))
            .retrieve()
            .toBodilessEntity()
    }

    private data class ReserveRequest(val orderId: String, val productId: String, val quantity: Int)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ReserveResponse(val reservationId: String)
    private data class ReleaseRequest(val orderId: String, val reservationId: String)
}
