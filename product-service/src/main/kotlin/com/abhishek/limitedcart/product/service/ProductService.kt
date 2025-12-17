package com.abhishek.limitedcart.product.service

import com.abhishek.limitedcart.common.exception.ResourceNotFoundException
import com.abhishek.limitedcart.product.entity.ProductEntity
import com.abhishek.limitedcart.product.entity.ProductView
import com.abhishek.limitedcart.product.messaging.ProductEventPublisher
import com.abhishek.limitedcart.product.repository.ProductRepository
import com.abhishek.limitedcart.product.service.dto.CreateProductRequest
import com.abhishek.limitedcart.product.service.dto.UpdateProductRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productEventPublisher: ProductEventPublisher,
    private val priceRedisService: PriceRedisService
) {

    @Transactional
    fun createProduct(request: CreateProductRequest): ProductView {
        val product = ProductEntity(
            name = request.name,
            description = request.description,
            price = request.price,
            maxQuantityPerSale = request.maxQuantityPerSale,
            active = request.active
        )
        val saved = productRepository.save(product)
        
        // Sync price to Redis if product is active
        if (saved.active) {
            priceRedisService.setPrice(saved.id.toString(), saved.price)
        }
        
        productEventPublisher.publishProductCreated(saved)
        return saved.toView()
    }

    @Transactional(readOnly = true)
    fun getProduct(id: UUID): ProductView =
        productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Product $id not found") }
            .toView()

    @Transactional(readOnly = true)
    fun listProducts(pageable: Pageable): Page<ProductView> =
        productRepository.findAll(pageable).map { it.toView() }

    @Transactional
    fun updateProduct(id: UUID, request: UpdateProductRequest): ProductView {
        val product = productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Product $id not found") }
        
        val priceChanged = product.price != request.price
        val activeChanged = product.active != request.active
        
        product.name = request.name
        product.description = request.description
        product.price = request.price
        product.active = request.active
        
        val saved = productRepository.save(product)
        
        // Update Redis based on active status and price changes
        when {
            saved.active && (priceChanged || activeChanged) -> {
                // Product is active and price changed or became active
                priceRedisService.setPrice(saved.id.toString(), saved.price)
            }
            !saved.active && activeChanged -> {
                // Product became inactive
                priceRedisService.deletePrice(saved.id.toString())
            }
        }
        
        productEventPublisher.publishProductUpdated(saved)
        return saved.toView()
    }

    @Transactional
    fun deleteProduct(id: UUID) {
        // Remove price from Redis before deleting
        priceRedisService.deletePrice(id.toString())
        productRepository.deleteById(id)
    }
}
