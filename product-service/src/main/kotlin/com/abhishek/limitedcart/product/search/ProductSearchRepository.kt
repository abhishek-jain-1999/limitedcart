package com.abhishek.limitedcart.product.search

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, String> {
    fun findByNameContainingIgnoreCase(name: String): List<ProductDocument>
}
