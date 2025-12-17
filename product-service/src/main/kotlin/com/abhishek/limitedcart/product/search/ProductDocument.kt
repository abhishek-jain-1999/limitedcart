package com.abhishek.limitedcart.product.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.math.BigDecimal
import java.time.LocalDateTime

@Document(indexName = "products_index")
data class ProductDocument(
    @Id val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val active: Boolean,
    val inStock: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
