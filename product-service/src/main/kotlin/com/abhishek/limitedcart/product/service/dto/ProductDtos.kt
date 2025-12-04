package com.abhishek.limitedcart.product.service.dto

import com.abhishek.limitedcart.product.entity.ProductView
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val description: String,
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = false)
    val price: BigDecimal,
    @field:Min(1)
    val maxQuantityPerSale: Int,
    val active: Boolean = true
)

data class UpdateProductRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val description: String,
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = false)
    val price: BigDecimal,
    val active: Boolean
)

data class ProductResponse(
    val product: ProductView
)

data class ProductListResponse(
    val items: List<ProductView>,
    val page: Int,
    val size: Int,
    val totalElements: Long
)
