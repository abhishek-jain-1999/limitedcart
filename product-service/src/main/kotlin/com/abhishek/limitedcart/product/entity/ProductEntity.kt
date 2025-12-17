package com.abhishek.limitedcart.product.entity

import com.abhishek.limitedcart.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID
import java.time.ZoneId

@Entity
@Table(name = "products")
data class ProductEntity(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "text")
    var description: String,

    @Column(nullable = false, precision = 19, scale = 2)
    var price: BigDecimal,

    @Column(name = "max_quantity_per_sale", nullable = false)
    var maxQuantityPerSale: Int,

    @Column(nullable = false)
    var active: Boolean = true,
    @Column(name = "in_stock", nullable = false)
    var inStock: Boolean = false
) : BaseEntity() {

    fun toView(): ProductView =
        ProductView(
            id = this.id,
            name = name,
            description = description,
            price = price,
            maxQuantityPerSale = maxQuantityPerSale,
            active = active,
            inStock = inStock,
            createdAt = createdAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            updatedAt = updatedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
}

data class ProductView(
    val id: UUID?,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val maxQuantityPerSale: Int,
    val active: Boolean,
    val inStock: Boolean,
    val createdAt: Long?,
    val updatedAt: Long?
)
