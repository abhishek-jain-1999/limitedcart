package com.abhishek.limitedcart.inventory.repository

import com.abhishek.limitedcart.inventory.entity.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ReservationRepository : JpaRepository<Reservation, UUID> {
    fun findByOrderId(orderId: String): Optional<Reservation>
}

