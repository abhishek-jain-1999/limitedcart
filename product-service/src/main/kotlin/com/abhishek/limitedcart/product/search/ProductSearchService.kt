package com.abhishek.limitedcart.product.search

import com.abhishek.limitedcart.common.constants.KafkaTopics
import com.abhishek.limitedcart.common.events.ProductCreatedEvent
import com.abhishek.limitedcart.common.events.ProductUpdatedEvent
import com.abhishek.limitedcart.product.entity.ProductEntity
import com.abhishek.limitedcart.product.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProductSearchService(
    private val repository: ProductSearchRepository
) {
    fun indexProduct(product: ProductEntity) {
        val document = ProductDocument(
            id = requireNotNull(product.id).toString(),
            name = product.name,
            description = product.description,
            price = product.price,
            active = product.active,
            inStock = product.inStock,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
        repository.save(document)
    }

    fun search(query: String): List<ProductDocument> =
        repository.findByNameContainingIgnoreCase(query)
}

@Component
class ProductEventListener(
    private val productRepository: ProductRepository,
    private val searchService: ProductSearchService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopics.PRODUCT_CREATED], groupId = "product-search")
    fun onProductCreated(event: ProductCreatedEvent) {
        handleEvent(event.productId)
    }

    @KafkaListener(topics = [KafkaTopics.PRODUCT_UPDATED], groupId = "product-search")
    fun onProductUpdated(event: ProductUpdatedEvent) {
        handleEvent(event.productId)
    }

    private fun handleEvent(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            log.warn("Invalid product id {}", id)
            return
        }
        productRepository.findById(uuid)
            .ifPresentOrElse(
                searchService::indexProduct,
                { log.warn("Product {} not found during indexing", uuid) }
            )
    }
}
