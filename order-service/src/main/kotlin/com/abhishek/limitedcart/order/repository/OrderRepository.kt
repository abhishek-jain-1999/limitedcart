package com.abhishek.limitedcart.order.repository

import com.abhishek.limitedcart.order.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<OrderEntity, UUID>
