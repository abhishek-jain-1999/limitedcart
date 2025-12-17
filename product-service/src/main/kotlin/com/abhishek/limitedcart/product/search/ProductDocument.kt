package com.abhishek.limitedcart.product.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.math.BigDecimal

@Document(indexName = "products_index")
data class ProductDocument(
    @Id val id: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val active: Boolean,
    val inStock: Boolean,
    
    @Field(type = FieldType.Date, format = [DateFormat.epoch_millis])
    val createdAt: Long?,
    
    @Field(type = FieldType.Date, format = [DateFormat.epoch_millis])
    val updatedAt: Long?
)
