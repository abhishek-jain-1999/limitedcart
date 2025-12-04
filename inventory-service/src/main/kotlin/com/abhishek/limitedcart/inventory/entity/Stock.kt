package com.abhishek.limitedcart.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "stock")
class Stock(
    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    var productId: String,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int,

    @Version
    @Column(nullable = false)
    var version: Long = 0
)

